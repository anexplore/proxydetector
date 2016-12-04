package com.fd.proxydetector.http;

import org.apache.commons.io.IOUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;


public class DefaultHttpClientFactory extends AbstractHttpClientFactory<AsyncHttpClient> {
    
    private final AsyncHttpClient httpClient;
    
    public DefaultHttpClientFactory() {
        this(10_000, 10_000);
    }
    
    public DefaultHttpClientFactory(int connectTimeout, int readTimeout) {
        //其它各个配置参数可以通过修改ahc.properties修改
        AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setAcceptAnyCertificate(true)
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .setKeepAlive(true)
                .build();
        httpClient = new DefaultAsyncHttpClient(config);
    }

    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }

    public void close() {
        IOUtils.closeQuietly(httpClient);
    }
}
