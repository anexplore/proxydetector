package com.fd.proxydetector.httpdetector;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fd.proxydetector.DetectorTask;
import com.fd.proxydetector.utils.TimeUtils;

public class DetectorBossWorker extends AbstractDetectorWorker {
    
    private final Selector selector;
    private final ArrayList<DetectorSubWorker> subWorkers;
    private final CountDownLatch stopLatch;
    private final LinkedList<DetectorTask> tasks;
    private final LinkedList<SelectionKey> cancelledKeys;
    private int roundIndex;
    private volatile boolean stopWorker;
    
    public DetectorBossWorker() throws IOException {
        selector = Selector.open();
        subWorkers = new ArrayList<>(Runtime.getRuntime().availableProcessors());
        stopLatch = new CountDownLatch(1);
        tasks = new LinkedList<>();
        cancelledKeys = new LinkedList<>();
    }
    
    public void registerTask(DetectorTask task) {
        synchronized(tasks) {
            tasks.add(task);
        }
        
    }
    
    @Override
    public void cancelDetectorTask(DetectorTask task) {
        synchronized(cancelledKeys) {
            if (task.attachedSelectionKey() != null) {
                cancelledKeys.add(task.attachedSelectionKey());
            }
        }
    }
    
    private void processAddTasks() {
        synchronized(tasks) {
            for (DetectorTask task : tasks) {
                if (task.isDone()) {
                    continue;
                }
                SocketChannel channel = null;
                while (true) { 
                    try {
                        channel = createChannel(task);
                        break;
                    } catch (IOException e) {
                        TimeUtils.wait(2, TimeUnit.SECONDS);
                    }
                }
                try {
                    connect(channel, task);
                } catch (IOException e) {
                    e.printStackTrace();
                    closeChannel(channel);
                    task.fail();
                }
            }
            if (tasks.size() > 0) {
                selector.wakeup();
            }
            tasks.clear();
        }
    }
    
    private void processCancelledTasks() {
        synchronized(cancelledKeys) {
            for (SelectionKey skey : cancelledKeys) {
                releaseResource(skey, skey.channel());
            }
            cancelledKeys.clear();
        }
    }
    
    public void registerSubWorker(DetectorSubWorker subWorker) {
        if (subWorker != null) {
            synchronized(subWorkers) {
                subWorkers.add(subWorker);
            }
        }
    }
    
    private SocketChannel createChannel(DetectorTask task) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        return channel;
    }
    
    private void connect(SocketChannel channel, DetectorTask task)
            throws IOException {
        SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);
        key.attach(task);
        task.attachSelectionKey(key);
        task.attachDetectorWorker(this);
        try {
            if (channel.connect(task.address)) {
                key.cancel();
                registerToSubWorker(channel, task);
            }
        } catch (IOException e) {
            key.cancel();
            throw e;
        }
    }
    
    private void registerToSubWorker(SocketChannel channel, DetectorTask task) {
        synchronized(subWorkers) {
            if (++roundIndex >= subWorkers.size()) {
                roundIndex = 0;
            }
            if (subWorkers.size() > 0) {
                task.attachDetectorWorker(null);
                task.attachSelectionKey(null);
                if (!subWorkers.get(roundIndex).register(channel, task)) {
                    task.fail();
                }
            }
        }
    }
    
    public void shutdown() {
        stopWorker = true;
        try {
            stopLatch.await();
        } catch (InterruptedException ignore) {
        }
    }
    
    private void closeSelector() {
        synchronized(this) {
            closeSelector(selector);
        }
    }

    @Override
    public void run() {
        while (!stopWorker) {
            processCancelledTasks();
            processAddTasks();
            try {
                if (selector.select(500) > 0) {
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        SocketChannel channel = (SocketChannel)key.channel();
                        DetectorTask task = (DetectorTask)key.attachment();
                        if (key.isConnectable()) {
                            try {
                                if (channel.finishConnect()) {
                                    key.cancel();
                                    registerToSubWorker(channel, task);
                                }
                            } catch (IOException e) {
                                releaseResource(key, channel);
                                task.fail();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                stopWorker = true;
            }
        }
        closeSelector();
        stopLatch.countDown();
    }

}
