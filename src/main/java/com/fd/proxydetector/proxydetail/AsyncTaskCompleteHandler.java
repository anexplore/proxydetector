package com.fd.proxydetector.proxydetail;

public interface AsyncTaskCompleteHandler<T> {
    /**
     * 任务完成
     * @param t
     */
    public void complete(T t);
}
