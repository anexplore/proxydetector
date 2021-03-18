package com.fd.proxyscan;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.fd.proxyscan.utils.IPUtils;

public class MultiPortDetectAddressProvider implements DetectAddressProvider {

    private byte[] startIp;
    private int[] ports;
    private int portIndex;

    public MultiPortDetectAddressProvider(InetAddress startAddress, int[] ports) {
        startIp = startAddress.getAddress();
        this.ports = ports;
        portIndex = 0;
    }

    private void incIp() {
        IPUtils.changeToNextIp(startIp);
    }

    @Override
    public InetSocketAddress next() {
        if (portIndex == ports.length) {
            portIndex = 0;
            incIp();
        }
        try {
            return new InetSocketAddress(InetAddress.getByAddress(startIp), ports[portIndex++]);
        } catch (UnknownHostException ignore) {
        }
        return null;
    }

}
