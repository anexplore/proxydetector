package com.fd.proxydetector;

import java.io.UnsupportedEncodingException;

public class Constants {
    public static String DETECT_HTTP_URL = "http://hm.baidu.com/hm.gif";
    public static String DETECT_HTTP_REQUEST = "GET " + DETECT_HTTP_URL + " HTTP/1.1\r\n"
            + "Host:hm.baidu.com\r\n"
            + "Connection:close\r\n"
            + "\r\n";
    public static int HTTP_PROXY_EXPECTED_RESPONSE_BODY_SIZE = 43;
    public static byte[] DETECT_HTTP_REQUEST_BYTE;
    static {
        try {
            DETECT_HTTP_REQUEST_BYTE = DETECT_HTTP_REQUEST.getBytes("utf8");
        } catch (UnsupportedEncodingException e) {
            DETECT_HTTP_REQUEST_BYTE = DETECT_HTTP_REQUEST.getBytes();
        }
    }

}
