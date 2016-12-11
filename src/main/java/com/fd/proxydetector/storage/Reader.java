package com.fd.proxydetector.storage;

import java.io.IOException;

import com.fd.proxydetector.Proxy;

public interface Reader {
    
    /**
     * 读取一个Proxy
     * @return Proxy Proxy 如果读取完毕返回null
     * @throws IOException
     */
    public Proxy read() throws IOException;

    /**
     * 关闭
     * @throws IOException
     */
    public void close() throws IOException;
}
