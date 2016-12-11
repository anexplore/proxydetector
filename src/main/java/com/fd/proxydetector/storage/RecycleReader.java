package com.fd.proxydetector.storage;

import java.io.IOException;

public interface RecycleReader extends Reader {
    
    /**
     * 重置
     * */
    public void recycle() throws IOException;
}
