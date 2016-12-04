package com.fd.proxydetector;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProxyTest {
    
    @Test
    public void testFullLocation() {
        Proxy proxy = new Proxy();
        assertEquals(proxy.getFullLocation(), "");
    }
    
    @Test
    public void testProxyType() {
        Proxy proxy = new Proxy();
        assertFalse(proxy.isHttpSupported());
        proxy.proxyType = Proxy.HTTP;
        assertTrue(proxy.isHttpSupported());
        proxy.proxyType = Proxy.HTTP | Proxy.HTTPS;
        assertTrue(proxy.isHttpsSupported());
        assertTrue(proxy.isHttpSupported());
    }
}
