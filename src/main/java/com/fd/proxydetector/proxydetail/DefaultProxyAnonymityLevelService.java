package com.fd.proxydetector.proxydetail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.proxy.ProxyServer;

import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.ProxyAnonymityLevel;
import com.fd.proxydetector.http.AbstractHttpClientFactory;

import io.netty.handler.codec.http.HttpResponseStatus;

public class DefaultProxyAnonymityLevelService implements ProxyAnonymityLevelService {

    public static final String PROXY_JUDGE_URL = "http://proxyjudge.us";
    public static final Pattern REMOTEADDRPTN = Pattern.compile("^REMOTE_ADDR\\s=\\s(.*)$",
            Pattern.MULTILINE);
    public static final Pattern HTTPVIAPTN = Pattern.compile("^HTTP_VIA\\s=\\s(.*)$",
            Pattern.MULTILINE);
    public static final Pattern HTTPFORWARDEDFORPTN =
            Pattern.compile("^HTTP_X_FORWARDED_FOR\\s=\\s(.*)$", Pattern.MULTILINE);
    private final AsyncHttpClient httpClient;
    // 本机外网IP地址
    private final String localIp;
    
    public DefaultProxyAnonymityLevelService(
            AbstractHttpClientFactory<AsyncHttpClient> httpClientFactory,
            String localIp) {
        httpClient = httpClientFactory.getHttpClient();
        this.localIp = localIp;
    }
    
    private boolean containsSpecHost(String body) {
        return body.contains("HTTP_HOST = proxyjudge.us");
    }
    
    private String getRemoteAddrHeader(String body) {
        Matcher matcher = REMOTEADDRPTN.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    
    private String getHttpViaHeader(String body) {
        Matcher matcher = HTTPVIAPTN.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    
    private String getHttpXForwardedForHeader(String body) {
        Matcher matcher = HTTPFORWARDEDFORPTN.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    } 

    private ProxyAnonymityLevel processResponseBody(String body) {
        String remoteAddr = getRemoteAddrHeader(body);
        String httpVia = getHttpViaHeader(body);
        String forwardedFor = getHttpXForwardedForHeader(body);
        // 如果没有REMOTEADDR则认为代理有问题
        if (remoteAddr.isEmpty()) {
            return null;
        }
        if (!httpVia.isEmpty()) {
           if (forwardedFor.trim().equals(localIp)) {
               // 对于没有固定IP的扫描机器 此判断是有问题的
               return ProxyAnonymityLevel.TRANSPARENT;
           } else {
               return ProxyAnonymityLevel.ANONYMOUS;
           }
        }
        return ProxyAnonymityLevel.ELITE;
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
            if (response == null) {
                return null;
            }
            String body = response.getResponseBody();
            if (StringUtils.isEmpty(body)) {
                return null;
            }
            if (response.getStatusCode() != HttpResponseStatus.OK.code() 
                    || !containsSpecHost(body)) {
                return null;
            }
            return processResponseBody(body);
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public void resolve(Proxy proxy, final AsyncTaskCompleteHandler<ProxyAnonymityLevel> handler) {
        if (handler == null) {
            return;
        }
        if (proxy == null || StringUtils.isBlank(proxy.host)) {
            handler.complete(null);
        }
        Request request = new RequestBuilder()
                .setUrl(PROXY_JUDGE_URL)
                .setProxyServer(new ProxyServer.Builder(proxy.host, proxy.port))
                .build();
        httpClient.executeRequest(request, new AsyncCompletionHandler<Response>() {

            @Override
            public Response onCompleted(Response response) throws Exception {
                if (response == null) {
                    handler.complete(null);
                }
                String body = response.getResponseBody();
                if (StringUtils.isBlank(body)) {
                    handler.complete(null);
                }
                if (response.getStatusCode() != HttpResponseStatus.OK.code() 
                        || !containsSpecHost(body)) {
                    handler.complete(null);
                } else {
                    handler.complete(processResponseBody(body));
                }
                return response;
            }
            
            @Override
            public void onThrowable(Throwable t){
                handler.complete(null);
            }
        });
    }

}
