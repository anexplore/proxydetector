package com.fd.proxydetector.proxydetail.updater;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.alibaba.fastjson.JSON;
import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.storage.RecycleReader;

public class FileProxyStorageReader implements RecycleReader {
    private String filePath;
    private boolean opened;
    private BufferedReader reader;
    
    private FileProxyStorageReader(String filePath) {
        this.filePath = filePath;
    }
    
    private void openFile() throws IOException {
        opened = false;
        if (reader != null) {
            reader.close();
        }
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath),
                "utf8"));
        opened = true;
    }
    
    @Override
    public Proxy read() throws IOException {
        if (!opened) {
            openFile();
        }
        String proxyLine = reader.readLine();
        if (proxyLine == null) {
            return null;
        }
        return JSON.parseObject(proxyLine, Proxy.class);
    }

    @Override
    public void close() throws IOException {
        opened = false;
        if (reader != null) {
            reader.close();
        }
    }

    @Override
    public void recycle() throws IOException {
        openFile();
    }

}
