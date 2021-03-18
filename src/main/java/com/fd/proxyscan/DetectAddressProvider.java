package com.fd.proxyscan;

import java.net.InetSocketAddress;

public interface DetectAddressProvider {

    /**
     * @return next address
     */
    InetSocketAddress next();
}
