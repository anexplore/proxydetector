package com.fd.proxydetector.proxydetail.updater;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.utils.TimeUtils;

public class UpdaterTask {
    
    public enum State {
        PREPARE_DETECT_ANONYMITY,
        DETECTING_ANONYMITY,
        PREPARE_DETECT_LOCATION,
        DETECTING_LOCATION,
        FINISHED
    }
    
    private final Proxy proxy;
    private final AtomicLong startTime;
    private final AtomicLong endTime;
    private final AtomicReference<State> state;
    
    public UpdaterTask(Proxy proxy) {
        assert proxy != null;
        this.proxy = proxy;
        this.startTime = new AtomicLong(TimeUtils.monotonicNow());
        this.endTime = new AtomicLong(TimeUtils.monotonicNow() - 1000L);
        this.state = new AtomicReference<State>(State.PREPARE_DETECT_ANONYMITY);
    }
    
    public void startDetectAnonymity() {
        if (state.compareAndSet(State.PREPARE_DETECT_ANONYMITY, State.DETECTING_ANONYMITY)) {
            startTime.getAndSet(TimeUtils.monotonicNow());
        }
    }
    
    public void endDetectAnonymity() {
        if (state.compareAndSet(State.DETECTING_ANONYMITY, State.PREPARE_DETECT_LOCATION)) {
            endTime.getAndSet(TimeUtils.monotonicNow());
        }
    }
    
    public void startDetectLocation() {
        if (state.compareAndSet(State.PREPARE_DETECT_LOCATION, State.DETECTING_LOCATION)) {
            startTime.getAndSet(TimeUtils.monotonicNow());
        }
    }
    
    public void endDetectLocation() {
        if (state.compareAndSet(State.DETECTING_LOCATION, State.FINISHED)) {
            endTime.getAndSet(TimeUtils.monotonicNow());
        }
    }
    
    public void finish() {
        state.getAndSet(State.FINISHED);
    }
    
    public Proxy getProxy() {
        return proxy;
    }
    
    public long getStartTime() {
        return startTime.get();
    }
    
    public long getEndTime() {
        return endTime.get();
    }
    
    public State getState() {
       return state.get();
    }
    
    public long calcSpeed() {
        return endTime.get() - startTime.get();
    }
}
