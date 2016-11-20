package com.fd.proxydetector.utils;

public interface TimerTaskInterface {
    
    /**
     * @return 超时时间戳
     */
    long getTimeoutstamp();
    
    /**
     * @return 是否已经被取消
     */
    boolean isCancelled();
    
    /**
     * @return 触发超时
     */
    void trigerTimeout();
    
    /**
     * @return 是否已经完成
     */
    boolean isDone();
}
