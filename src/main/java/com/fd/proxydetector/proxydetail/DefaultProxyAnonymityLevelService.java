package com.fd.proxydetector.proxydetail;

import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.asynchttpclient.proxy.ProxyServer;

import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.ProxyAnonymityLevel;
import com.fd.proxydetector.http.AbstractHttpClientFactory;

public class DefaultProxyAnonymityLevelService implements ProxyAnonymityLevelService {

    public static final String PROXY_JUDGE_URL = "http://proxyjudge.us";

    private final AsyncHttpClient httpClient;

    public DefaultProxyAnonymityLevelService(
            AbstractHttpClientFactory<AsyncHttpClient> httpClientFactory) {
        httpClient = httpClientFactory.getHttpClient();
    }

    @Override
    public ProxyAnonymityLevel resolve(Proxy proxy) {
        if (proxy == null || StringUtils.isBlank(proxy.host) || proxy.port <= 0) {
            return null;
        }
        try {
            ListenableFuture<Response> responseFuture = httpClient.prepareGet(PROXY_JUDGE_URL)
                    .setProxyServer(new ProxyServer.Builder(proxy.host, proxy.port)).execute();
            Response response = responseFuture.get();
            String body = response.getResponseBody();
            //TODO: parse body, resolve anonymity level
        } catch (Exception e) {
        }
        return null;
    }

}
