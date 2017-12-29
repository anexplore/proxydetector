package com.fd.proxyscan.store;

import com.fd.proxyscan.Proxy;

public interface ProxyStore {
	/**
	 * 存储Proxy
	 * @param proxy 代理
	 * @return 保存成功true 否则 false
	 */
	boolean save(Proxy proxy);
}
