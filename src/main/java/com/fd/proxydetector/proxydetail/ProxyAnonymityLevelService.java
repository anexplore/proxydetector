package com.fd.proxydetector.proxydetail;

import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.ProxyAnonymityLevel;

public interface ProxyAnonymityLevelService {
    
    /**
     * 测试代理匿名等级
     * @param proxy Proxy
     * @return ProxyAnonymityLevel 代理等级
     */
    public ProxyAnonymityLevel resolve(Proxy proxy);
}
