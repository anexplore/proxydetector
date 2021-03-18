package com.fd.proxyscan;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.fd.proxyscan.utils.IPUtils;

public class DefaultDetectAddressProvider implements DetectAddressProvider {

    private byte[] startIp;
    private int port;

    public DefaultDetectAddressProvider(InetAddress startAddress, int port) {
        this.startIp = startAddress.getAddress();
        this.port = port;
    }

    private void incIp() {
        IPUtils.changeToNextIp(startIp);
    }

    @Override
    public InetSocketAddress next() {
        incIp();
        try {
            return new InetSocketAddress(InetAddress.getByAddress(startIp), port);
        } catch (UnknownHostException ignore) {
        }
        return null;
    }

}
