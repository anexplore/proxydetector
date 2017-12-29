package com.fd.proxyscan.store;

import com.fd.proxyscan.Proxy;

public class StdoutProxyStore implements ProxyStore {

	@Override
	public boolean save(Proxy proxy) {
		System.out.println(String.format("----\n%s\t%s\t%d", proxy.getProtocol(),
				proxy.getHost(), proxy.getPort()));
		System.out.println(proxy);
		System.out.println();
		return true;
	}

}
