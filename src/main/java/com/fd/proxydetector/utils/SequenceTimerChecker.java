package com.fd.proxydetector.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 时间序的超时检测
 * 没有使用HashedWheelTimer
 * @author caoliuyi
 *
 */
public class SequenceTimerChecker implements Runnable {
    
    private final LinkedList<TimerTaskInterface> tasks;
    private final ConcurrentLinkedQueue<TimerTaskInterface> waitTasks;
    private final ScheduledThreadPoolExecutor executor;
    private long spin = 50L;
    
    public SequenceTimerChecker() {
        tasks = new LinkedList<>();
        waitTasks = new ConcurrentLinkedQueue<TimerTaskInterface>();
        executor = new ScheduledThreadPoolExecutor(1);
    }
    
    public void startup() {
        executor.scheduleWithFixedDelay(this, 0, spin, TimeUnit.MILLISECONDS);
    }
    
    public void shutdown() {
        executor.shutdownNow();
    }
    
    public void addTask(TimerTaskInterface task) {
        if (task != null) {
            waitTasks.add(task);
        }
    }
    
    private void check() {
        for (int i = 0; i < 100000; i++) {
            TimerTaskInterface task = waitTasks.poll();
            if (task == null) {
                break;
            }
            if (task.isDone()) {
                continue;
            }
            // 忽略等待时间消耗
            tasks.add(task);
        }
        if (tasks.size() == 0) {
            return;
        }
        // 比较粗糙 在并发不高情况下遍历耗时可控
        Iterator<TimerTaskInterface> iter = tasks.iterator();
        while (iter.hasNext()) {
            TimerTaskInterface task = iter.next();
            if (task.isDone()) {
                iter.remove();
                continue;
            }
            if (task.getTimeoutstamp() <= TimeUtils.monotonicNow()) {
                task.trigerTimeout();
                iter.remove();
            }
        }
    }
    
    public void run() {
        check();
    }
}
