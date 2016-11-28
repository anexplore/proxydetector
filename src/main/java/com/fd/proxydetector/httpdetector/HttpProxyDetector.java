package com.fd.proxydetector.httpdetector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.fd.proxydetector.DetectorTask;
import com.fd.proxydetector.utils.SequenceTimerChecker;

public class HttpProxyDetector {
       
    private static final int CORE_SIZE = Runtime.getRuntime().availableProcessors();

    private ThreadPoolExecutor executor;
    
    private byte[] startIp;
    
    private int maxConcurrentCount;
    
    private Semaphore sem;
    
    private DetectorBossWorker bossWorker;
    
    private DetectorSubWorker[] subWorkers;
    
    private SequenceTimerChecker timerChecker;
    
    private volatile boolean stop;
    
    private int[] COMMON_PORT = new int[] {808, 3128, 818, 9000, 8123, 8998, 8118};
    
    public HttpProxyDetector(String ip, int maxConcurrentCount) throws UnknownHostException {
        this.startIp = InetAddress.getByName(ip).getAddress();
        this.maxConcurrentCount = maxConcurrentCount;
    }

    public void init() throws IOException {
        this.sem = new Semaphore(maxConcurrentCount);
        this.timerChecker = new SequenceTimerChecker();
        this.executor = new ThreadPoolExecutor(CORE_SIZE + 1, CORE_SIZE + 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2));
        bossWorker = new DetectorBossWorker();
        subWorkers = new DetectorSubWorker[CORE_SIZE - 1 > 0 ? CORE_SIZE - 1 : 1];
        executor.submit(bossWorker);
        for (int i = 0; i < subWorkers.length; i++) {
            DetectorSubWorker sub = new DetectorSubWorker(maxConcurrentCount);
            bossWorker.registerSubWorker(sub);
            executor.submit(sub);
            subWorkers[i] = sub;
        }
        addShutdownHook();
    }
    
    private void stopDetector() {
        stop = true;
        timerChecker.shutdown();
        bossWorker.shutdown();
        timerChecker.shutdown();
        for (int i = 0; i < subWorkers.length; i++) {
            subWorkers[i].shutdown();
        }
        executor.shutdown();
    }
    
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                stopDetector();
            }
        });
    }
    
    public void start() throws IOException {
        timerChecker.startup();
        long count = 0;
        while(!stop) {
            incIp();
            for (int i = 0; i < COMMON_PORT.length; i++) {
                while (!stop) {
                    try {
                        if (!sem.tryAcquire(1, TimeUnit.SECONDS)) {
                            continue;
                        }
                        break;
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                if (stop) {
                    break;
                }
                count++;
                DetectorTask task = new DetectorTask(
                        new InetSocketAddress(InetAddress.getByAddress(startIp), COMMON_PORT[i]),
                        ByteBuffer.wrap(Constants.DETECT_HTTP_REQUEST_BYTE),
                        sem, 10000); 
                bossWorker.registerTask(task);
                timerChecker.addTask(task);
                if (count % 1000 == 0) {
                    System.out.println(count);
                }
            }
            
        }
    }
    
    private void incIp() {
        for (int i = 3; i >= 0; i--) {
            if ((startIp[i] & 0xFF) < 254) {
                startIp[i]++;
                break;
            } else {
                startIp[i] = 1;
            }
        }
        int top = (startIp[0] & 0xFF); 
        if (top == 172 || top == 192) {
            startIp[0]++;
        }
    }
    
    public static void main(String args[]) throws IOException {
        if (args.length < 2) {
            System.out.println("Wrong Param\nParams:start_ip max_concurrent\n");
            System.exit(1);
        }
        HttpProxyDetector detector = new HttpProxyDetector(args[0],
                Integer.parseInt(args[1]));
        detector.init();
        detector.start();
    }
}
