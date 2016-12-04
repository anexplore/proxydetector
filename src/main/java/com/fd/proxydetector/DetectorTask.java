package com.fd.proxydetector;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.fd.proxydetector.http.HttpResponse;
import com.fd.proxydetector.http.SimpleHttpDecoder;
import com.fd.proxydetector.httpdetector.AbstractDetectorWorker;
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
    private Boolean released;
    
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
        onTimeout();
        releaseSource();
    }

    @Override
    public boolean isDone() {
        return isDone.get();
    }
    
    public void cancel() {
        isDone.getAndSet(true);
        cancelled.getAndSet(true);
        onCancelled();
        releaseSource();
    }
    
    public void fail() {
        isDone.getAndSet(true);
        onFail();
        releaseSource();
    }
 
    public void success() {
        isDone.getAndSet(true);
        success.getAndSet(true);
        onSuccess();
        releaseSource();
    }

    public boolean isSuccess() {
        return success.get();
    }
    
    private void onFail() {
        //do nothing now
    }
    
    //executed by sub worker thread
    //do not do too much computation
    private void onSuccess() {
        byte[] data = stream.toByteArray();
        if (data.length > Constants.HTTP_PROXY_EXPECTED_RESPONSE_BODY_SIZE) {
            HttpResponse response = SimpleHttpDecoder.decode(data);
            if (response.body != null 
                    && response.body.length == Constants.HTTP_PROXY_EXPECTED_RESPONSE_BODY_SIZE) {
                System.err.println(address.toString());
                System.err.flush();
            }
        }
    }
    
    private void onTimeout() {
        onCancelled();
    }
    
    private void onCancelled() {
        AbstractDetectorWorker worker = workerRef.get();
        if (worker != null) {
            worker.cancelDetectorTask(this);
        }
    }
    
    private void releaseSource() {
        skeyRef.getAndSet(null);
        workerRef.getAndSet(null);
        // only release once
        synchronized(released) {
            if (!released) {
                sem.release();
                released = true;
            }
        }
    }
}