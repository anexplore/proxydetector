package com.fd.proxydetector.storage;

import java.io.IOException;

import com.fd.proxydetector.Proxy;

public interface Writer {
    
    /**
     * 写出Proxy
     * @param proxy
     * @return 写出成功true 否则 false
     * @throws IOException 发生IO错误
     */
    public void write(Proxy proxy) throws IOException;

    /**
     * 关闭
     * @throws IOException
     */
    public void close() throws IOException;
}