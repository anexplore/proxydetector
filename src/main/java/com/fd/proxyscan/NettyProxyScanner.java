package com.fd.proxyscan;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.fd.proxyscan.store.ProxyStore;
import com.fd.proxyscan.store.StdoutProxyStore;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.socksx.v4.*;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;

import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fd.proxyscan.Constants.*;

public class NettyProxyScanner {
    private static final Logger LOG = LoggerFactory.getLogger(NettyProxyScanner.class);
    private final NettyProxyScanConfig config;
    private final DetectAddressProvider provider;
    private final ProxyStore proxyStore;

    private Bootstrap httpBootstrap;
    private Bootstrap socks4Bootstrap;
    private Bootstrap socks5Bootstrap;
    private SslContext sslCtx;
    private EventLoopGroup eventLoopGroup;

    private Semaphore connSemaphore;
    private Thread detectThread;
    private Thread proxyStoreThread;

    @Deprecated
    private final LinkedBlockingQueue<Proxy> httpsDetectQueue;
    private final LinkedBlockingQueue<Proxy> proxiesStoreQueue;

    private volatile boolean stop;

    public static void main(String[] args) throws Exception {
        String startIp = args[0];
        int[] ports = Arrays.stream(args[1].split(",")).mapToInt(Integer::parseInt).toArray();
        int maxConn = Integer.parseInt(args[2]);
        LOG.info("startip: {}, ports: {}, maxconn: {}", startIp, ports, maxConn);
        MultiPortDetectAddressProvider provider = new MultiPortDetectAddressProvider(
                InetAddress.getByName(startIp), ports);
        NettyProxyScanConfig config = new NettyProxyScanConfig();
        config.maxDetectConnections = maxConn;
        NettyProxyScanner scanner = new NettyProxyScanner(config, provider);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    scanner.close();
                } catch (InterruptedException ignore) {
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
        this.connSemaphore = new Semaphore(config.maxDetectConnections);
    }

    private void bootstrap() throws Exception {
        eventLoopGroup = new NioEventLoopGroup(config.eventLoopThreadNumber);
        sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        httpBootstrap = newBootstrap().handler(new PipelineInitializer());
        socks4Bootstrap = newBootstrap().handler(new SocksPipelineInitializer(false));
        socks5Bootstrap = newBootstrap().handler(new SocksPipelineInitializer(true));

        proxyStoreThread = new Thread(this::storeProxiesLoop);
        detectThread = new Thread(this::detectProxyLoop);

        detectThread.setName("Detect");
        proxyStoreThread.setName("Proxy-Store");
        detectThread.start();
        proxyStoreThread.start();
    }

    public void close() throws InterruptedException {
        stop = true;
        if (detectThread != null) {
            detectThread.join();
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        if (proxyStoreThread != null) {
            proxyStoreThread.join();
        }
    }

    private boolean running() {
        // more clearly
        return stop == false;
    }

    private void pushToStoreQueue(Proxy proxy) {
        try {
            proxiesStoreQueue.put(proxy);
        } catch (InterruptedException ignore) {
        }
    }

    @Deprecated
    private void pushToHttpsDetectQueue(Proxy proxy) {
        try {
            httpsDetectQueue.put(proxy);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * must call after check content readableBytes length is bigger than six
     *
     * @param content HttpContent
     * @return true if is correct gif image header
     */
    private boolean isCorrectGifHeader(HttpContent content) {
        byte[] header = new byte[6];
        content.content().readBytes(header);
        return header[0] == GIF_89A_HEADER[0] && header[1] == GIF_89A_HEADER[1] && header[2] == GIF_89A_HEADER[2]
                && header[3] == GIF_89A_HEADER[3] && header[4] == GIF_89A_HEADER[4] && header[5] == GIF_89A_HEADER[5];
    }

    private Bootstrap newBootstrap() {
        return new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectTimeout)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true);
    }

    private Proxy newProxy(ProxyProtocol proxyProtocol, InetSocketAddress address) {
        Proxy proxy = new Proxy(proxyProtocol, address.getHostName(), address.getPort());
        proxy.setFoundDate(new Date()).setLastCheckDate(new Date()).setValid(1);
        return proxy;
    }

    private void storeProxiesLoop() {
        while (running() || proxiesStoreQueue.size() > 0) {
            Proxy proxy = null;
            try {
                proxy = proxiesStoreQueue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
            }
            if (proxy == null) {
                continue;
            }
            proxyStore.save(proxy);
        }
    }

    private void detectProxyLoop() {
        long count = 0;
        while (running()) {
            InetSocketAddress address = provider.next();
            if (address == null) {
                stop = true;
                break;
            }
            count++;
            if (count % 100 == 0) {
                LOG.info("processed {}'th address: {}", count, address);
            }
            try {
                detectProxy(address);
            } catch (InterruptedException ignore) {
            }
        }
        LOG.info("total detect {} address", count);
    }

    private void detectHttpsProxy(InetSocketAddress proxy) {
        httpBootstrap.connect(proxy).addListener((ChannelFutureListener) future -> {
            future.channel().closeFuture().addListener((ChannelFutureListener) future1 -> connSemaphore.release());
            if (!future.isSuccess()) {
                future.channel().close();
                return;
            }
            future.channel().attr(DETECT_PROGRESS).set(DetectProgress.CONNECT);
            future.channel().writeAndFlush(PROXY_HTTPS_CONNECT_REQUEST);
        });
    }

    private void detectSocks4Proxy(InetSocketAddress proxy) {
        socks4Bootstrap.connect(proxy).addListener((ChannelFutureListener) future -> {
            future.channel().closeFuture().addListener((ChannelFutureListener) future1 -> connSemaphore.release());
            if (!future.isSuccess()) {
                future.channel().close();
                return;
            }
            future.channel().attr(DETECT_PROGRESS).set(DetectProgress.CONNECT);
            // this will detect socks4a
            future.channel().writeAndFlush(new DefaultSocks4CommandRequest(Socks4CommandType.CONNECT,
                    REQUEST_HOST, REQUEST_PORT));
        });
    }

    private void detectSocks5Proxy(InetSocketAddress proxy) {
        socks5Bootstrap.connect(proxy).addListener((ChannelFutureListener) future -> {
            future.channel().closeFuture().addListener((ChannelFutureListener) future1 -> connSemaphore.release());
            if (!future.isSuccess()) {
                future.channel().close();
                return;
            }
            future.channel().attr(DETECT_PROGRESS).set(DetectProgress.CONNECT);
            future.channel().writeAndFlush(SOCKS_5_INITIAL_REQUEST);
        });
    }

    private void detectProxy(InetSocketAddress proxy) throws InterruptedException {
        if (proxy == null) {
            return;
        }
        connSemaphore.acquire(1);
        detectHttpsProxy(proxy);
        connSemaphore.acquire(1);
        detectSocks5Proxy(proxy);
        connSemaphore.acquire(1);
        detectSocks4Proxy(proxy);
    }

    private class HttpProxyScannerHandler extends SimpleChannelInboundHandler<HttpObject> {

        @Deprecated
        private void handleHttpProgress(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        	if (msg instanceof HttpResponse) {
        		if (((HttpResponse)msg).status().code() != HttpResponseStatus.OK.code()) {
        			ctx.close();
        			return;
        		}
        	}
        	if (msg instanceof LastHttpContent) {
                HttpContent content = (HttpContent) msg;
                if (content.content().readableBytes() == EXPECTED_CONTENT_LENGTH && isCorrectGifHeader(content)) {
                    InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                    Proxy proxy = newProxy(ProxyProtocol.HTTP, address);
                    LOG.info("find http-proxy: {}:{}", address.getHostName(), address.getPort());
                    pushToStoreQueue(proxy);
                    pushToHttpsDetectQueue(proxy);
                }
            }
        	// we use http object aggregator so first http response object is complete, just close connection
            ctx.close();
        }

        private void handleHttpConnectProgress(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;
                if (response.status().code() == HttpResponseStatus.OK.code()) {
                    ctx.pipeline().remove(HTTP_CODEC);
                    ctx.pipeline().addAfter(READ_TIMEOUT, HTTP_CODEC, new HttpClientCodec());
                    ctx.pipeline().addAfter(READ_TIMEOUT, SSL, sslCtx.newHandler(ctx.alloc()));
                    ctx.channel().attr(DETECT_PROGRESS).set(DetectProgress.HTTPS);
                    ctx.writeAndFlush(PROXY_HTTPS_REQUEST).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                } else {
                    ctx.close();
                }
            }
        }

        private void handleHttpsProgress(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        	if (msg instanceof HttpResponse) {
        		if (((HttpResponse)msg).status().code() != HttpResponseStatus.OK.code()) {
        			ctx.close();
        			return;
        		}
        	}
        	if (msg instanceof LastHttpContent) {
                HttpContent content = (HttpContent) msg;
                if (content.content().readableBytes() == EXPECTED_CONTENT_LENGTH && isCorrectGifHeader(content)) {
                    InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                    Proxy proxy = newProxy(ProxyProtocol.HTTPS, address);
                    LOG.info("find https-proxy: {}:{}", address.getHostName(), address.getPort());
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
                    // disabled
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

    private class Socks5ProxyScannerHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            DetectProgress progress = ctx.channel().attr(DETECT_PROGRESS).get();
            // socks connect
            if (progress == DetectProgress.CONNECT) {
                if (msg instanceof Socks5InitialResponse) {
                    Socks5InitialResponse initialResponse = (Socks5InitialResponse) msg;
                    if (initialResponse.authMethod().equals(Socks5AuthMethod.NO_AUTH)) {
                        // Connect Request
                        ctx.pipeline().replace(DECODER, DECODER, new Socks5CommandResponseDecoder());
                        ctx.writeAndFlush(new DefaultSocks5CommandRequest(
                                Socks5CommandType.CONNECT,
                                Socks5AddressType.DOMAIN,
                                REQUEST_HOST,
                                REQUEST_PORT
                        )).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                    } else {
                        // drop
                        ctx.close();
                    }
                } else if (msg instanceof Socks5CommandResponse) {
                    Socks5CommandResponse commandResponse = (Socks5CommandResponse) msg;
                    if (commandResponse.status().equals(Socks5CommandStatus.SUCCESS)) {
                        ctx.channel().attr(DETECT_PROGRESS).set(DetectProgress.HTTP);
                        // Send Http Request
                        ctx.pipeline().addAfter(READ_TIMEOUT, SSL, sslCtx.newHandler(ctx.alloc()));
                        ctx.pipeline().replace(ENCODER, ENCODER, new HttpClientCodec());
                        ctx.pipeline().replace(DECODER, DECOMPRESSED, new HttpContentDecompressor());
                        ctx.pipeline().addAfter(DECOMPRESSED, AGGREGATION, new HttpObjectAggregator(MAX_HTTP_AGGREGATION_LENGTH));
                        ctx.writeAndFlush(PROXY_HTTPS_REQUEST).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                    } else {
                        ctx.close();
                    }
                } else {
                    ctx.close();
                }
            } else {
                // http test
                // both check status code and content length; and gif header
                if (msg instanceof HttpResponse) {
                    if (((HttpResponse)msg).status().code() != HttpResponseStatus.OK.code()) {
                        ctx.close();
                        return;
                    }
                }
                if (msg instanceof LastHttpContent) {
                    HttpContent content = (HttpContent) msg;
                    if (content.content().readableBytes() == EXPECTED_CONTENT_LENGTH && isCorrectGifHeader(content)) {
                        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                        // getHostName() will trigger reverse ip to hostname
                        Proxy proxy = newProxy(ProxyProtocol.SOCKS5, address);
                        LOG.info("find socks5-proxy: {}:{}", address.getHostName(), address.getPort());
                        pushToStoreQueue(proxy);
                        pushToHttpsDetectQueue(proxy);
                    }
                }
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }

    private class Socks4ProxyScannerHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            DetectProgress progress = ctx.channel().attr(DETECT_PROGRESS).get();
            if (progress == DetectProgress.CONNECT) {
                if (msg instanceof Socks4CommandResponse) {
                    Socks4CommandResponse commandResponse = (Socks4CommandResponse) msg;
                    if (commandResponse.status().equals(Socks4CommandStatus.SUCCESS)) {
                        ctx.channel().attr(DETECT_PROGRESS).set(DetectProgress.HTTP);
                        // Send Http Request
                        ctx.pipeline().addAfter(READ_TIMEOUT, SSL, sslCtx.newHandler(ctx.alloc()));
                        ctx.pipeline().replace(ENCODER, ENCODER, new HttpClientCodec());
                        ctx.pipeline().replace(DECODER, DECOMPRESSED, new HttpContentDecompressor());
                        ctx.pipeline().addAfter(DECOMPRESSED, AGGREGATION, new HttpObjectAggregator(MAX_HTTP_AGGREGATION_LENGTH));
                        ctx.writeAndFlush(PROXY_HTTPS_REQUEST).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                    } else {
                        ctx.close();
                    }
                }
            } else {
                // http test
                // both check status code and content length; and gif header
                if (msg instanceof HttpResponse) {
                    if (((HttpResponse)msg).status().code() != HttpResponseStatus.OK.code()) {
                        ctx.close();
                        return;
                    }
                }
                if (msg instanceof LastHttpContent) {
                    HttpContent content = (HttpContent) msg;
                    if (content.content().readableBytes() == EXPECTED_CONTENT_LENGTH && isCorrectGifHeader(content)) {
                        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                        // getHostName() will trigger reverse ip to hostname
                        Proxy proxy = newProxy(ProxyProtocol.SOCKS4A, address);
                        LOG.info("find socks4a-proxy: {}:{}", address.getHostName(), address.getPort());
                        pushToStoreQueue(proxy);
                        pushToHttpsDetectQueue(proxy);
                    }
                }
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }

    private class PipelineInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel sc) throws Exception {
            ChannelPipeline pipeline = sc.pipeline();
            pipeline.addLast(READ_TIMEOUT, new ReadTimeoutHandler((int) TimeUnit.MILLISECONDS.toSeconds(config.readTimeout)));
            pipeline.addLast(HTTP_CODEC, new HttpClientCodec());
            pipeline.addLast(DECOMPRESSED, new HttpContentDecompressor());
            // target image size is 43, 128K is bigger enough
            pipeline.addLast(AGGREGATION, new HttpObjectAggregator(MAX_HTTP_AGGREGATION_LENGTH));
            pipeline.addLast(PROXY_HANDLER, new HttpProxyScannerHandler());
            // add handler to process not accept message and release reference
            pipeline.addLast(new ChannelInboundHandlerAdapter() {
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    ReferenceCountUtil.release(msg);
                }
            });
        }
    }

    private class SocksPipelineInitializer extends ChannelInitializer<SocketChannel> {

        public final boolean socks5;

        public SocksPipelineInitializer(boolean socks5) {
            this.socks5 = socks5;
        }

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            ChannelPipeline pipeline = socketChannel.pipeline();
            pipeline.addLast(READ_TIMEOUT, new ReadTimeoutHandler((int)TimeUnit.MICROSECONDS.toSeconds(config.readTimeout)));
            if (socks5) {
                pipeline.addLast(ENCODER, Socks5ClientEncoder.DEFAULT);
                pipeline.addLast(DECODER, new Socks5InitialResponseDecoder());
                pipeline.addLast(PROXY_HANDLER, new Socks5ProxyScannerHandler());
            } else {
                pipeline.addLast(ENCODER, Socks4ClientEncoder.INSTANCE);
                pipeline.addLast(DECODER, new Socks4ClientDecoder());
                pipeline.addLast(PROXY_HANDLER, new Socks4ProxyScannerHandler());
            }
            pipeline.addLast(new ChannelInboundHandlerAdapter() {
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    ReferenceCountUtil.release(msg);
                }
            });
        }
    }
}
