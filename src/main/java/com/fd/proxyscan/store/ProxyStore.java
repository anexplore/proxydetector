package com.fd.proxyscan.store;

import com.fd.proxyscan.Proxy;

public interface ProxyStore {
	/**
	 * save proxy
	 * @param proxy proxy
	 * @return true if success else false
	 */
	boolean save(Proxy proxy);
}
