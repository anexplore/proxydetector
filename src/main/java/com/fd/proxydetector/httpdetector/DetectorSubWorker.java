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

import com.fd.proxydetector.DetectorTask;

public class DetectorSubWorker extends AbstractDetectorWorker {
    
    private final Selector selector;
    private final CountDownLatch stopLatch;
    private final LinkedList<ChannelTaskPair> tasks;
    private final LinkedList<DetectorTask> cancelledTasks;
    private volatile boolean stopWorker;
    
    public DetectorSubWorker() throws IOException {
        selector = Selector.open();
        stopLatch = new CountDownLatch(1);
        tasks = new LinkedList<>();
        cancelledTasks = new LinkedList<>();
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
        synchronized(cancelledTasks) {
            cancelledTasks.add(task);
        }
    }
    
    private void processCancelledTasks() {
        synchronized(cancelledTasks) {
            for (DetectorTask task : cancelledTasks) {
                SelectionKey skey = task.attachedSelectionKey();
                if (skey != null) {
                    closeChannel((SocketChannel)skey.channel());
                    skey.cancel();
                }
            }
            cancelledTasks.clear();
        }
    }
    
    public void register(SocketChannel channel, DetectorTask task) {
        if (channel == null || !channel.isOpen()) {
            System.err.println("channel is not open yet");
            return;
        }
        synchronized(tasks) {
            tasks.add(new ChannelTaskPair(channel, task));
        }
    }

    private void processAddTasks() {
        synchronized(tasks) {
            for (ChannelTaskPair pair : tasks) {
                SelectionKey key;
                try {
                    key = pair.channel.register(selector,
                            SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    key.attach(pair.task);
                } catch (ClosedChannelException e) {
                    pair.task.fail();
                }
            }
            if (tasks.size() > 0) {
                selector.wakeup();
            }
            tasks.clear();
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
            processAddTasks();
            processCancelledTasks();
            try {
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
                stopWorker = true;
            } catch (Exception e) {
                stopWorker = true;
            }
        }
        closeSelector();
        stopLatch.countDown();
    }

}
