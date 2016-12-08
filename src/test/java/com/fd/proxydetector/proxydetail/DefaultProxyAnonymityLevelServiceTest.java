package com.fd.proxydetector.proxydetail;

import org.asynchttpclient.AsyncHttpClient;
import org.junit.Test;

import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.ProxyAnonymityLevel;
import com.fd.proxydetector.http.AbstractHttpClientFactory;
import com.fd.proxydetector.http.DefaultHttpClientFactory;

public class DefaultProxyAnonymityLevelServiceTest {
    
    @Test
    public void testLookup() {
        Proxy proxy = new Proxy();
        proxy.host = "113.44.57.118";
        proxy.port = 9000;
        AbstractHttpClientFactory<AsyncHttpClient> factory = new DefaultHttpClientFactory();
        DefaultProxyAnonymityLevelService service = 
                new DefaultProxyAnonymityLevelService(factory, "");
        ProxyAnonymityLevel level = service.resolve(proxy);
        System.err.println(level);
        factory.close();
    }
}
