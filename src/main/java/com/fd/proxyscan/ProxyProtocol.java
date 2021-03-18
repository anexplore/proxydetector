package com.fd.proxyscan;

public enum ProxyProtocol {
    NO,
    HTTP, // only support http request
    HTTPS, // support http and https request
    SOCKS4,
    SOCKS4A,
    SOCKS5
}
