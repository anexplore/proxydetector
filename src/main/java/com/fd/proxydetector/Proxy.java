package com.fd.proxydetector;

public class Proxy {
    
    public enum ProxyType {
        HTTP,
        HTTPS,
        SOCKS,
        SOCKS5,
    }
    
    public ProxyType proxyType;
    public String host;
    public int port;
    
}
