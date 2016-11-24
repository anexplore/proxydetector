package com.fd.proxydetector.httpdetector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fd.proxydetector.DetectorTask;

public class DetectorSubWorker extends AbstractDetectorWorker {
    
    private final Selector selector;
    private final CountDownLatch stopLatch;
    private final LinkedBlockingQueue<ChannelTaskPair> tasks;
    private final LinkedList<SelectionKey> cancelledKeys;
    private volatile boolean stopWorker;
    
    public DetectorSubWorker() throws IOException {
        this(1000);
    }

    public DetectorSubWorker(int maxWaitSize) throws IOException {
        selector = Selector.open();
        stopLatch = new CountDownLatch(1);
        tasks = new LinkedBlockingQueue<>(maxWaitSize);
        cancelledKeys = new LinkedList<>();
    }
    
    public void shutdown() {
        stopWorker = true;
        try {
            stopLatch.await();
        } catch (InterruptedException ignore) {
        }
    }
    
    private static class ChannelTaskPair {
        public SocketChannel channel;
        public DetectorTask task;
        
        public ChannelTaskPair(SocketChannel channel, DetectorTask task) {
            this.channel = channel;
            this.task = task;
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
    
    private void processCancelledTasks() {
        synchronized(cancelledKeys) {
            for (SelectionKey skey : cancelledKeys) {
                releaseResource(skey, skey.channel());
            }
            cancelledKeys.clear();
        }
    }
    
    public boolean register(SocketChannel channel, DetectorTask task) {
        if (channel == null || !channel.isOpen()) {
            System.err.println("channel is not open yet");
            return false;
        }
        if (stopWorker) {
            return false;
        }
        ChannelTaskPair pair = new ChannelTaskPair(channel, task);
        try {
            while(!task.isDone() && !tasks.offer(pair, 500, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            task.fail();
        }
        return true;
    }

    private void processAddTasks() {
        int i = 0;
        for (; i < tasks.size(); i++) {
            ChannelTaskPair pair = tasks.poll();
            if (pair == null) {
                break;
            }
            if (pair.task.isDone()) {
                continue;
            }
            SelectionKey key;
            try {
                key = pair.channel.register(selector,
                        SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                key.attach(pair.task);
                pair.task.attachDetectorWorker(this);
                pair.task.attachSelectionKey(key);
            } catch (ClosedChannelException e) {
                pair.task.fail();
            }
        }
        if (i > 0) {
            selector.wakeup();
        }
    }
    
    private void closeSelector() {
        synchronized(this) {
            closeSelector(selector);
        }
    }
    
    private void disableWrite(SelectionKey key) {
        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
    }
    
    @Override
    public void run() {
        while(!stopWorker) {
            try {
                processCancelledTasks();
                processAddTasks();
                if (selector.select(500) > 0) {
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        SocketChannel channel = (SocketChannel)key.channel();
                        DetectorTask task = (DetectorTask)key.attachment();
                        if (key.isWritable()) {
                            try {
                                while (task.requestBuffer.hasRemaining()) {
                                    channel.write(task.requestBuffer);
                                }                                 
                            } catch (IOException e) {
                                releaseResource(key, channel);
                                task.fail();
                                continue;
                            }
                            disableWrite(key);
                        }
                        if (key.isReadable()) {
                            ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                            int len = 0;
                            try {
                                while ((len = channel.read(readBuffer)) > 0) {
                                    readBuffer.flip();
                                    byte[] buffer = new byte[len];
                                    readBuffer.get(buffer);
                                    task.stream.write(buffer);
                                    readBuffer.clear();
                                } 
                            } catch (IOException e) {
                                releaseResource(key, channel);
                                task.fail();
                            }
                            if (len < 0) {
                                releaseResource(key, channel);
                                task.success();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                stopWorker = true;
            } catch (Exception e) {
                e.printStackTrace();
                stopWorker = true;
            }
        }
        closeSelector();
        stopLatch.countDown();
    }

}
