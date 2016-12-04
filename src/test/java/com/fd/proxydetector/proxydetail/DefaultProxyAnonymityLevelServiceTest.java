package com.fd.proxydetector.proxydetail;

import static org.junit.Assert.assertFalse;

import org.apache.commons.lang3.StringUtils;
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
        proxy.host = "120.132.9.140";
        proxy.port = 8123;
        AbstractHttpClientFactory<AsyncHttpClient> factory = new DefaultHttpClientFactory();
        DefaultProxyAnonymityLevelService service = new DefaultProxyAnonymityLevelService(factory);
        ProxyAnonymityLevel level = service.resolve(proxy);
        factory.close();
    }
}
