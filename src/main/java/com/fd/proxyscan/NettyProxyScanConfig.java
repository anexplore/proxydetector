package com.fd.proxyscan;

public class NettyProxyScanConfig {
    /* ms */
    public int connectTimeout = 2000;
    /* ms */
    public int readTimeout = 2000;
    /* set to zero to use default cpu core number*/
    public int eventLoopThreadNumber = 0;
    
    public int maxHttpDetectConc = 30000;
    public int maxHttpsDetectConc = 20;
}
