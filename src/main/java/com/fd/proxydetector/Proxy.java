package com.fd.proxydetector;

public class Proxy {
    
    public static final int HTTP = 1;
    public static final int HTTPS = 2;
    public static final int SOCKS = 4;
    public static final int SOCKS5 = 8;
    /*代理类型*/
    public int proxyType;
    /*地址*/
    public String host;
    /*端口*/
    public int port;
    /*地理位置*/
    public ProxyLocation location;
    /*匿名等级*/
    public ProxyAnonymityLevel anonymity;
    /*访问速度*/
    public long speed;
    
    /**
     * <p>详细地理位置信息:</p>
     * <p>country area region city county</p>
     * @return 详细地理位置信息
     */
    public String getFullLocation() {
        return location == null ? "" : location.getFullLocation();
    }
    
    /**
     * @return 是否HTTP代理
     */
    public boolean isHttpSupported() {
        return (proxyType & HTTP) == HTTP;
    }
    
    /**
     * @return 是否HTTPS代理
     */
    public boolean isHttpsSupported() {
        return (proxyType & HTTPS) == HTTPS;
    }
    
    /**
     * @return 是否SOCKS代理
     */
    public boolean isSocksSupported() {
        return (proxyType & SOCKS) == SOCKS;
    }
    
    /**
     * @return 是否SOCKS5代理
     */
    public boolean isSocks5Supported() {
        return (proxyType & SOCKS5) == SOCKS5;
    }
}
