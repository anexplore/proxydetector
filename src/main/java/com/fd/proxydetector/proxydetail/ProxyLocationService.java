package com.fd.proxydetector.proxydetail;

import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.ProxyLocation;

public interface ProxyLocationService {
    
    /**
     * 查找Proxy的地理位置信息
     * @param proxy 需要查找的Proxy
     * @return ProxyLocation or null if fail
     */
    ProxyLocation lookup(Proxy proxy);
    
    /**
     * 查找Proxy地址位置信息 结果由handler处理
     * @param proxy
     * @param handler
     */
    void lookup(Proxy proxy, AsyncTaskCompleteHandler<ProxyLocation> handler);
}
