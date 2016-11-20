package com.fd.proxydetector.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

 private static final long NANOSECONDS_PER_MILLISECOND = 1000000;
    
    /**
     * @return 当前时间戳 毫秒级别
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * 等待timeout的时间
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    public static void wait(long timeout, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(timeout));
        } catch (Exception ignore) {}
    }
    
    /**
     * @return 返回基于当前jvm的时间 不受系统底层时间改变的影响
     */
    public static long monotonicNow() {
        return System.nanoTime() / NANOSECONDS_PER_MILLISECOND;
    }
    
    /**
     * @return 返回当前时间Date
     */
    public static Date getCurrentDate() {
        return new Date();
    }
}
