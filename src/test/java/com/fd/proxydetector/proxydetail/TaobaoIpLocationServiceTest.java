package com.fd.proxydetector.proxydetail;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Test;

import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.ProxyLocation;
import com.fd.proxydetector.http.AbstractHttpClientFactory;
import com.fd.proxydetector.http.DefaultHttpClientFactory;

public class TaobaoIpLocationServiceTest {

    @Test
    public void testLookup() {
        Proxy proxy = new Proxy();
        proxy.host = "120.84.156.94";
        AbstractHttpClientFactory<AsyncHttpClient> factory = new DefaultHttpClientFactory();
        TaobaoIpLocationService service = new TaobaoIpLocationService(factory);
        ProxyLocation location = service.lookup(proxy);
        assertFalse(StringUtils.isEmpty(location.getFullLocation()));
        factory.close();
    }
}
