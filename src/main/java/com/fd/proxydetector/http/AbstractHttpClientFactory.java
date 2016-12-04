package com.fd.proxydetector.http;

public abstract class AbstractHttpClientFactory<T> {

    public void close() {}
    
    public abstract T getHttpClient();
}
