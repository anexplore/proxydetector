package com.fd.proxydetector.proxydetail.updater;

import org.asynchttpclient.AsyncHttpClient;

import com.fd.proxydetector.http.AbstractHttpClientFactory;
import com.fd.proxydetector.http.DefaultHttpClientFactory;
import com.fd.proxydetector.proxydetail.DefaultProxyAnonymityLevelService;
import com.fd.proxydetector.proxydetail.ProxyAnonymityLevelService;
import com.fd.proxydetector.proxydetail.ProxyLocationService;
import com.fd.proxydetector.proxydetail.TaobaoIpLocationService;
import com.fd.proxydetector.storage.Reader;
import com.fd.proxydetector.storage.Writer;

public class ProxyDetailUpdaterMain {
    private Reader reader;
    private Writer writer;
    private ProxyLocationService locService;
    private ProxyAnonymityLevelService anonyService;
    private AbstractHttpClientFactory<AsyncHttpClient> clientFactory;
    private ProxyDetailUpdater updater;
    private String localIp;
    private String proxyInPath;
    private String proxyOutPath;
    
    private int maxAnonyConc = 5;
    private int maxLocConc = 5;
    
    public ProxyDetailUpdaterMain(String localIp, String proxyInPath, String proxyOutPath,
            int maxAnonyConc, int maxLocConc) {
        this.localIp = localIp;
        this.proxyInPath = proxyInPath;
        this.proxyOutPath = proxyOutPath;
        this.maxAnonyConc = maxAnonyConc;
        this.maxLocConc = maxLocConc;
        
    }
    
    public void init() {
        clientFactory = new DefaultHttpClientFactory();
        locService = new TaobaoIpLocationService(clientFactory);
        anonyService = new DefaultProxyAnonymityLevelService(clientFactory, localIp);
        reader = new FileProxyStorageReader(proxyInPath);
        writer = new FileProxyStorageWriter(proxyOutPath);
        updater = new ProxyDetailUpdater(reader, writer, locService, anonyService,
                maxAnonyConc, maxLocConc);
    }
    
    public void startup() {
        updater.start();
    }
    
    public void shutdown() {
        updater.shutdown();
    }
    
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }
    
    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Error params\nlocalIp proxyInPath proxyOutPath maxAnonyConc"
                    + " maxLocConc");
            System.exit(1);
        }
        ProxyDetailUpdaterMain updater = new ProxyDetailUpdaterMain(args[0], args[1], args[2],
                Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        updater.init();
        updater.startup();
        updater.addShutdownHook();
    }
}
