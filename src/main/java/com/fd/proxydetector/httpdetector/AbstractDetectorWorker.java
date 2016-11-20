package com.fd.proxydetector.httpdetector;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.commons.io.IOUtils;

import com.fd.proxydetector.DetectorTask;

public abstract class AbstractDetectorWorker implements Runnable {

    @Override
    public abstract void run();
    
    public abstract void shutdown();
    
    public abstract void cancelDetectorTask(DetectorTask task);
    
    public void closeChannel(SocketChannel channel) {
        try {
            channel.socket().shutdownOutput();
        } catch (IOException e) {
        }
        try {
            channel.socket().shutdownInput();
        } catch (IOException e) {
        }
        IOUtils.closeQuietly(channel.socket());
        IOUtils.closeQuietly(channel);
    }
    
    public void closeSelector(Selector selector) {
        for (SelectionKey key : selector.keys()) {
            SocketChannel channel = (SocketChannel)key.channel();
            key.cancel();
            IOUtils.closeQuietly(channel.socket());
            IOUtils.closeQuietly(channel);
        }
        IOUtils.closeQuietly(selector);
    }
    
    public void releaseResource(SelectionKey key, SelectableChannel channel) {
        if (key != null && key.isValid()) {
            key.cancel();
        }
        if (channel instanceof SocketChannel) {
            closeChannel((SocketChannel)channel); 
        }
    }
}
