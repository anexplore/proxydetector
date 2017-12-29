package com.fd.proxyscan;

import java.net.SocketAddress;

public interface DetectAddressProvider {

    /**
     * 如果返回null则终止探测
     * 
     * @return 下一个需要探测的网络地址
     */
    SocketAddress next();
}
