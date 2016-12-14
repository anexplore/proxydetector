package com.fd.proxydetector.proxydetail.updater;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.ProxyAnonymityLevel;
import com.fd.proxydetector.ProxyLocation;
import com.fd.proxydetector.proxydetail.AsyncTaskCompleteHandler;
import com.fd.proxydetector.proxydetail.ProxyAnonymityLevelService;
import com.fd.proxydetector.proxydetail.ProxyLocationService;
import com.fd.proxydetector.storage.Reader;
import com.fd.proxydetector.storage.RecycleReader;
import com.fd.proxydetector.storage.Writer;

public class ProxyDetailUpdater {
    public static final Logger LOG = LoggerFactory.getLogger(ProxyDetailUpdater.class);
    private final Reader reader;
    private final Writer writer;
    private final ProxyLocationService locService;
    private final ProxyAnonymityLevelService anonyService;
    private int maxAnonyConc = 5;
    private int maxLocConc = 5;
    private final LinkedBlockingQueue<UpdaterTask> locDetectionTasks;
    private final Semaphore anonySem;
    private final Semaphore locSem;
    private Thread detectAnonyThread;
    private Thread detectLocThread;
    private volatile boolean stopUpdater;
    private final CountDownLatch stopLatch;
    
    public ProxyDetailUpdater(final Reader reader, final Writer writer,
            final ProxyLocationService locService,
            final ProxyAnonymityLevelService anonyService,
            int maxAnonyConc, int maxLocConc) {
        anonySem = new Semaphore(maxAnonyConc);
        locSem = new Semaphore(maxLocConc);
        this.locService = locService;
        this.anonyService = anonyService;
        this.maxAnonyConc = maxAnonyConc;
        this.maxLocConc = maxLocConc;
        // max block 100
        locDetectionTasks = new LinkedBlockingQueue<UpdaterTask>(100);
        this.reader = reader;
        this.writer = writer;
        this.stopLatch = new CountDownLatch(2);
    }
 
    private Proxy readProxy() throws IOException {
        Proxy proxy = null;
        try {
            proxy = reader.read();
            if (proxy == null) {
                if (reader instanceof RecycleReader) {
                    ((RecycleReader)reader).recycle();
                }    
                proxy = reader.read();
            }
        } catch (IOException e) {
        }
        return proxy;
    }
    
    private UpdaterTask buildTask(Proxy proxy) {
        return new UpdaterTask(proxy);
    }
    
    private void writeProxy(Proxy proxy) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("write proxy:{}", proxy);
        }
        try {
            writer.write(proxy);
        } catch (IOException e) {
            LOG.error("write proxy fail", e);
        }
    }
    
    private void pushToLocDetectionQueue(UpdaterTask task) {
        try {
            locDetectionTasks.put(task);
        } catch (InterruptedException ignore) {
        }
    }
    
    private class AnonymityDetectionTaskCompleteHandler implements 
        AsyncTaskCompleteHandler<ProxyAnonymityLevel> {
        private final UpdaterTask task;
        
        public AnonymityDetectionTaskCompleteHandler(UpdaterTask task) {
            this.task = task;
        }
        
        @Override
        public void complete(ProxyAnonymityLevel level) {
            anonySem.release();
            task.endDetectAnonymity();
            Proxy proxy = task.getProxy();
            if (level == null) {
                // 代理有问题 或者 网络有问题
                task.finish();
                writeProxy(proxy);
                return;
            }
            // 代理等级
            proxy.anonymity = level;
            // 速度
            proxy.speed = task.calcSpeed();
            // 采集地理位置
            pushToLocDetectionQueue(task);
        }
    }
    
    private class LocationDetectionTaskCompleteHandler implements
        AsyncTaskCompleteHandler<ProxyLocation> {
        private final UpdaterTask task;
        
        public LocationDetectionTaskCompleteHandler(UpdaterTask task) {
            this.task = task;
        }

        @Override
        public void complete(ProxyLocation location) {
            locSem.release();
            Proxy proxy = task.getProxy();
            if (location == null) {
                // 代理有问题 或者 网络有问题
                task.finish();
                writeProxy(proxy);
                return;
            }
            proxy.location = location;
            // 采集地理位置
            task.finish();
            writeProxy(proxy);
        }
    }
    
    private void scheduleAnonymityTasks() {
        while (!stopUpdater) {
            try {
                if (!anonySem.tryAcquire(1, TimeUnit.SECONDS)) {
                    continue;
                }
            } catch (InterruptedException e) {
                continue;
            }
            Proxy proxy = null;
            try {
                proxy = readProxy();
            } catch (IOException e) {
                LOG.error("read proxy occurs error, break", e);
                break;
            }
            if (proxy == null) {
                LOG.warn("there is no proxy to read, break");
                break;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("start to process proxy:{}", proxy);
            }
            UpdaterTask task = buildTask(proxy);
            task.startDetectAnonymity();
            anonyService.resolve(proxy, new AnonymityDetectionTaskCompleteHandler(task));
        }
        stopLatch.countDown();
        LOG.info("anonymity detection thread exit");
    }
    
    private void scheduleLocationTasks() {
        while (!stopUpdater) {
            UpdaterTask task = null;
            try {
                task = locDetectionTasks.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e1) {
                continue;
            }
            if (task == null) {
                continue;
            }
            do {
                try {
                    if (locSem.tryAcquire(1, TimeUnit.SECONDS)) {
                        break;
                    }
                } catch (InterruptedException e) {
                }
            } while(true);
            task.startDetectLocation();
            locService.lookup(task.getProxy(), new LocationDetectionTaskCompleteHandler(task));
        }
        stopLatch.countDown();
        LOG.info("location detection thread exit");
    }
    
    public void start() {
        detectAnonyThread = new Thread() {
            public void run() {
                scheduleAnonymityTasks();
            }
        };
        
        detectLocThread = new Thread() {
            public void run() {
                scheduleLocationTasks();
            }
        };
        detectAnonyThread.start();
        detectLocThread.start();
    }
    
    public void shutdown() {
        stopUpdater = true;
        try {
            stopLatch.await();
        } catch (Exception ignore) {}
        LOG.info("updater exits");
    }
    
    public int getMaxAnonymityDetectionConc() {
        return maxAnonyConc;
    }
    
    public int getMaxLocationDetectionConc() {
        return maxLocConc;
    }
}
