package com.SixSense.queue;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class WorkerQueue implements Closeable {
    private static Logger logger = Logger.getLogger(WorkerQueue.class);
    private final ExecutorService workerPool = Executors.newCachedThreadPool();
    private boolean isClosed = false;

    @Autowired
    private WorkerQueue(){

    }

    public <V> Future<V> submit(Callable<V> worker) throws Exception{
        if(this.isClosed){
            throw new Exception("Cannot submit worker to work queue - worker pool has been closed");
        }
        return workerPool.submit(worker);
    }

    public void submit(Runnable worker) throws Exception{
        if(this.isClosed){
            throw new Exception("Cannot submit worker to work queue - worker pool has been closed");
        }
        workerPool.submit(worker);
    }

    public boolean isShutdown(){
        return workerPool.isShutdown();
    }

    @Override
    public void close() {
        this.workerPool.shutdownNow();
        this.isClosed = true;
        logger.info("WorkerQueue closed");
    }
}
