package com.fd.proxydetector;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.fd.proxydetector.httpdetector.AbstractDetectorWorker;
import com.fd.proxydetector.utils.HttpResponse;
import com.fd.proxydetector.utils.SimpleHttpDecoder;
import com.fd.proxydetector.utils.TimeUtils;
import com.fd.proxydetector.utils.TimerTaskInterface;

public class DetectorTask implements TimerTaskInterface {

    public final SocketAddress address;
    public final ByteBuffer requestBuffer;
    public final ByteArrayOutputStream stream;
    private final Semaphore sem;
    private final AtomicBoolean cancelled;
    private final AtomicBoolean isDone;
    private final AtomicBoolean success;
    private final AtomicReference<SelectionKey> skeyRef;
    private final AtomicReference<AbstractDetectorWorker> workerRef;
    private final long timeoutTimestamp;
    private volatile Boolean released;
    
    public DetectorTask(SocketAddress address, ByteBuffer requestBuffer,
            Semaphore sem, long timeout) {
        this.address = address;
        this.requestBuffer = requestBuffer;
        this.stream = new ByteArrayOutputStream(1024);
        this.timeoutTimestamp = timeout + TimeUtils.monotonicNow();
        this.sem = sem;
        this.released = new Boolean(false);
        this.cancelled = new AtomicBoolean(false);
        this.isDone = new AtomicBoolean(false);
        this.success = new AtomicBoolean(false);
        this.skeyRef = new AtomicReference<>(null);
        this.workerRef = new AtomicReference<>(null);
    }

    public void attachSelectionKey(SelectionKey key) {
        skeyRef.getAndSet(key);
    }
    
    public SelectionKey attachedSelectionKey() {
        return skeyRef.get();
    }
    
    public void attachDetectorWorker(AbstractDetectorWorker worker) {
        workerRef.getAndSet(worker);
    }
    
    public AbstractDetectorWorker attachedAbstractDetectorWorker() {
        return workerRef.get();
    }
    
    @Override
    public long getTimeoutstamp() {
        return timeoutTimestamp;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void trigerTimeout() {
        isDone.getAndSet(true);
        if (workerRef.get() != null) {
            workerRef.get().cancelDetectorTask(this);
        }
        releaseSem();
    }

    public void cancel() {
        isDone.getAndSet(true);
        cancelled.getAndSet(true);
        if (workerRef.get() != null) {
            workerRef.get().cancelDetectorTask(this);
        }
        releaseSem();
    }
    
    @Override
    public boolean isDone() {
        return isDone.get();
    }
    
    public void fail() {
        isDone.getAndSet(true);
        releaseSem();
    }
    
    public boolean isSuccess() {
        return success.get();
    }
    
    public void success() {
        isDone.getAndSet(true);
        success.getAndSet(true);
        byte[] data = stream.toByteArray();
        if (data.length > 50) {
            HttpResponse response = SimpleHttpDecoder.decode(data);
            if (response.body != null && response.body.length == 43) {
                System.err.println(new String(data));
                System.err.println(response.body.length);
            }
        }
        releaseSem();
    }
    
    private void releaseSem() {
        synchronized(released) {
            if (!released) {
                sem.release();
                released = true;
            }
        }
    }
}