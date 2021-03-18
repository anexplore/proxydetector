package com.fd.proxyscan;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public final class Constants {
    public static final String READ_TIMEOUT = "read timeout";
    public static final String HTTP_CODEC = "http codec";
    public static final String ENCODER = "encoder";
    public static final String DECODER = "decoder";
    public static final String SSL = "ssl";
    public static final String AGGREGATION = "aggregation";
    public static final String DECOMPRESSED = "decompressed";
    public static final String PROXY_HANDLER = "proxy handler";

    public static final int EXPECTED_CONTENT_LENGTH = 43;
    public static final String REQUEST_HOST = "hm.baidu.com";
    public static final int REQUEST_PORT = 443;
    public static final HttpRequest PROXY_HTTP_REQUEST = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1, HttpMethod.GET, "http://hm.baidu.com/hm.gif");
    public static final HttpRequest PROXY_HTTPS_CONNECT_REQUEST =
            new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, REQUEST_HOST + ":" + REQUEST_PORT);
    public static final HttpRequest PROXY_HTTPS_REQUEST =
            new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/hm.gif");
    public static final AttributeKey<DetectProgress> DETECT_PROGRESS = AttributeKey.valueOf("detectProgress");
    public static final AttributeKey<InetSocketAddress> DETECT_PROXY = AttributeKey.valueOf("detect-proxy");

    public static final Socks5InitialRequest SOCKS_5_INITIAL_REQUEST = new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH);
    public static final int  MAX_HTTP_AGGREGATION_LENGTH = 128 * 1024;
    // GIF89a
    public static final byte[] GIF_89A_HEADER = new byte[] {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};

    static {
        PROXY_HTTP_REQUEST.headers().set(HttpHeaderNames.HOST, "hm.baidu.com");
        PROXY_HTTPS_CONNECT_REQUEST.headers().set(HttpHeaderNames.HOST, "hm.baidu.com:443");
        PROXY_HTTPS_CONNECT_REQUEST.headers().set(HttpHeaderNames.PROXY_CONNECTION, "keep-alive");
        PROXY_HTTPS_REQUEST.headers().set(HttpHeaderNames.HOST, "hm.baidu.com");
    }

    public enum DetectProgress {
        HTTP, CONNECT, HTTPS
    }
}
