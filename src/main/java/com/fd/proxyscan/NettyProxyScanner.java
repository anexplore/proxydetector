package com.fd.proxyscan;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.fd.proxyscan.store.ProxyStore;
import com.fd.proxyscan.store.StdoutProxyStore;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;

@SuppressWarnings("deprecation")
public class NettyProxyScanner {

    private final NettyProxyScanConfig config;
    private final DetectAddressProvider provider;
    private final ProxyStore proxyStore;
    private Bootstrap bootstrap;
    private SslContext sslCtx;
    private EventLoopGroup eventLoopGroup;
    private Semaphore httpConc;
    private Semaphore httpsConc;
    private volatile boolean stop = false;
    private volatile boolean stopHttps = false;
    private Thread httpDetectThread;
    private Thread httpsDetectThread;
    private Thread proxyStoreThread;
    private final LinkedBlockingQueue<Proxy> httpsDetectQueue;
    private final LinkedBlockingQueue<Proxy> proxiesStoreQueue;
    private static final int EXPECTED_CONTENT_LENGTH = 43;
    private static final HttpRequest PROXY_HTTP_REQUEST = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1, HttpMethod.GET, "http://hm.baidu.com/hm.gif");
    private static final HttpRequest PROXY_HTTPS_CONNECT_REQUEST =
            new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "hm.baidu.com:443");
    private static final HttpRequest PROXY_HTTPS_REQUEST =
            new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/hm.gif");
    private static final AttributeKey<DetectProgress> DETECT_PROGRESS =
            AttributeKey.valueOf("detectProgress");

    private enum DetectProgress {
        HTTP, CONNECT, HTTPS
    }

    static {
        PROXY_HTTP_REQUEST.headers().set(HttpHeaderNames.HOST, "hm.baidu.com");
        PROXY_HTTPS_CONNECT_REQUEST.headers().set(HttpHeaderNames.HOST, "hm.baidu.com:443");
        PROXY_HTTPS_CONNECT_REQUEST.headers().set(HttpHeaderNames.PROXY_CONNECTION, "keep-alive");
        PROXY_HTTPS_REQUEST.headers().set(HttpHeaderNames.HOST, "hm.baidu.com");
    }

    public static void main(String[] args) throws Exception {
        MultiPortDetectAddressProvider provider = new MultiPortDetectAddressProvider(
                InetAddress.getByName(args[0]),
                Arrays.asList(args[1].split(",")).stream().mapToInt(Integer::parseInt).toArray());
        NettyProxyScanConfig config = new NettyProxyScanConfig();
        config.maxHttpDetectConc = Integer.parseInt(args[2]);
        NettyProxyScanner scanner = new NettyProxyScanner(config, provider);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    scanner.close();
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
            }
        });
        scanner.bootstrap();
    }

    public NettyProxyScanner(NettyProxyScanConfig config, DetectAddressProvider provider) {
        this.httpsDetectQueue = new LinkedBlockingQueue<>();
        this.proxiesStoreQueue = new LinkedBlockingQueue<>();
        this.proxyStore = new StdoutProxyStore();
        this.config = config;
        this.provider = provider;
        httpConc = new Semaphore(config.maxHttpDetectConc);
        httpsConc = new Semaphore(config.maxHttpsDetectConc);
    }

    private void bootstrap() throws Exception {
        this.eventLoopGroup = new NioEventLoopGroup(config.eventLoopThreadNumber);
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectTimeout)
                .option(ChannelOption.TCP_NODELAY, true).handler(new PipelineIntializer());
        this.sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        proxyStoreThread = new Thread() {
            public void run() {
                storeProxiesLoop();
            }
        };
        httpDetectThread = new Thread() {
            public void run() {
                detectHttpLoop();
            }
        };
        httpsDetectThread = new Thread() {
            public void run() {
                detectHttpsLoop();
            }
        };

        httpDetectThread.start();
        httpsDetectThread.start();
        proxyStoreThread.start();
    }

    public void close() throws IOException, InterruptedException {
        stop = true;
        if (httpDetectThread != null) {
            httpDetectThread.join();
        }
        if (httpsDetectThread != null) {
            httpsDetectThread.join();
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        if (proxyStoreThread != null) {
            proxyStoreThread.join();
        }
    }

    private void pushToStoreQueue(Proxy proxy) {
        try {
            proxiesStoreQueue.put(proxy);
        } catch (InterruptedException e) {
        }
    }

    private void pushToHttpsDetectQueue(Proxy proxy) {
        try {
            httpsDetectQueue.put(proxy);
        } catch (InterruptedException e) {
        }
    }

    private void storeProxiesLoop() {
        while (!stop || !proxiesStoreQueue.isEmpty()) {
            Proxy proxy = null;
            try {
                proxy = proxiesStoreQueue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            if (proxy == null) {
                continue;
            }
            proxyStore.save(proxy);
        }
    }

    private void detectHttpLoop() {
        int count = 0;
        while (!stop) {
            SocketAddress address = provider.next();
            if (address == null) {
                stop = true;
                break;
            }
            if (address != null) {
                count++;
                if (count % 20000 == 0) {
                    System.out.println(count + "," + address);
                }
                try {
                    detectProxy(address);
                } catch (InterruptedException ignore) {
                }
            }
        }
        stopHttps = true;
    }

    private void detectHttpsLoop() {
        while (!stopHttps || httpsDetectQueue.size() > 0) {
            Proxy httpProxy;
            try {
                httpProxy = httpsDetectQueue.poll(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
                continue;
            }
            if (httpProxy == null) {
                continue;
            }
            try {
                detectProxys(new InetSocketAddress(httpProxy.getHost(), httpProxy.getPort()));
            } catch (InterruptedException ignore) {
            }
        }
    }

    private void detectProxy(SocketAddress proxy) throws InterruptedException {
        if (proxy == null) {
            return;
        }
        httpConc.acquire();
        bootstrap.connect(proxy).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                future.channel().closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        httpConc.release();
                    }

                });
                if (!future.isSuccess()) {
                    future.channel().close();
                    return;
                }
                Channel channel = future.channel();
                channel.attr(DETECT_PROGRESS).set(DetectProgress.HTTP);
                channel.writeAndFlush(PROXY_HTTP_REQUEST);
            }
        });
    }

    private void detectProxys(SocketAddress proxy) throws InterruptedException {
        if (proxy == null) {
            return;
        }
        httpsConc.acquire();
        bootstrap.connect(proxy).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                future.channel().closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        httpsConc.release();
                    }
                });
                if (!future.isSuccess()) {
                    future.channel().close();
                    return;
                }
                Channel channel = future.channel();
                channel.attr(DETECT_PROGRESS).set(DetectProgress.CONNECT);
                channel.writeAndFlush(PROXY_HTTPS_CONNECT_REQUEST);
            }
        });
    }


    private class ProxyScannerHandler extends SimpleChannelInboundHandler<HttpObject> {

        private void handleHttpProgress(ChannelHandlerContext ctx, HttpObject msg)
                throws Exception {
            if (msg instanceof LastHttpContent) {
                HttpContent content = (HttpContent) msg;
                if (content.content().readableBytes() == EXPECTED_CONTENT_LENGTH) {
                    InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                    Proxy proxy =
                            new Proxy(ProxyProtocol.HTTP, address.getHostName(), address.getPort());
                    proxy.setFoundDate(new Date()).setLastCheckDate(new Date())
                            .setIsValid(Short.valueOf("1"));
                    System.err.println(
                            "HTTP-PROXY:" + address.getHostName() + "," + address.getPort());
                    pushToStoreQueue(proxy);
                    pushToHttpsDetectQueue(proxy);
                }
            }
            ctx.close();
        }

        private void handleHttpConnectProgress(ChannelHandlerContext ctx, HttpObject msg)
                throws Exception {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;
                if (response.status().code() == HttpResponseStatus.OK.code()) {
                    ctx.pipeline().remove("httpcodec");
                    ctx.pipeline().addAfter("readtimeout", "httpcodec", new HttpClientCodec());
                    ctx.pipeline().addAfter("readtimeout", "ssl", sslCtx.newHandler(ctx.alloc()));
                    ctx.channel().attr(DETECT_PROGRESS).set(DetectProgress.HTTPS);
                    ctx.writeAndFlush(PROXY_HTTPS_REQUEST).addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (!future.isSuccess()) {
                                ctx.close();
                            }
                        }
                    });
                } else {
                    ctx.close();
                }
            }
        }

        private void handleHttpsProgress(ChannelHandlerContext ctx, HttpObject msg)
                throws Exception {
            if (msg instanceof LastHttpContent) {
                HttpContent content = (HttpContent) msg;
                if (content.content().readableBytes() == EXPECTED_CONTENT_LENGTH) {
                    InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                    Proxy proxy = new Proxy(ProxyProtocol.HTTPS, address.getHostName(),
                            address.getPort());
                    proxy.setFoundDate(new Date()).setLastCheckDate(new Date())
                            .setIsValid(Short.valueOf("1"));
                    System.err.println(
                            "HTTPS-PROXY:" + address.getHostName() + "," + address.getPort());
                    pushToStoreQueue(proxy);
                }
            }
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
            Channel channel = ctx.channel();
            DetectProgress progress = channel.attr(DETECT_PROGRESS).get();
            switch (progress) {
                case HTTP:
                    handleHttpProgress(ctx, msg);
                    break;
                case CONNECT:
                    handleHttpConnectProgress(ctx, msg);
                    break;
                case HTTPS:
                    handleHttpsProgress(ctx, msg);
                    break;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }

    private class PipelineIntializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel sc) throws Exception {
            ChannelPipeline p = sc.pipeline();
            p.addLast("readtimeout", new ReadTimeoutHandler(
                    (int) TimeUnit.MILLISECONDS.toSeconds(config.readTimeout)));
            p.addLast("httpcodec", new HttpClientCodec());
            p.addLast("decompress", new HttpContentDecompressor());
            p.addLast("aggre", new HttpObjectAggregator(1024 * 1024));
            p.addLast("proxyhandler", new ProxyScannerHandler());
        }

    }
}
