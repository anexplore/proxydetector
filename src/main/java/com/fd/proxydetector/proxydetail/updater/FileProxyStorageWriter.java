package com.fd.proxydetector.proxydetail.updater;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.alibaba.fastjson.JSON;
import com.fd.proxydetector.Proxy;
import com.fd.proxydetector.storage.Writer;

public class FileProxyStorageWriter implements Writer {

    private BufferedWriter writer;
    private String filePath;
    private boolean opened;
    
    
    public FileProxyStorageWriter(String filePath) {
        this.filePath = filePath;
    }
    
    private void openFile() throws IOException {
        opened = false;
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath),
                "utf8"));
        opened = true;
    }
    
    @Override
    public void write(Proxy proxy) throws IOException {
        if (!opened) {
            openFile();
        }
        writer.write(JSON.toJSONString(proxy) + "\n");
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

}
