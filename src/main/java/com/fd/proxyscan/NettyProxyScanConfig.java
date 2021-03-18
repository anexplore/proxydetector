package com.fd.proxyscan;

public class NettyProxyScanConfig {
    /* ms */
    public int connectTimeout = 2000;
    /* ms */
    public int readTimeout = 2000;
    /* set to zero to use default cpu core number */
    public int eventLoopThreadNumber = 0;
    /*max connections, this can control bandwidth*/
    public int maxDetectConnections = 30000;
}
