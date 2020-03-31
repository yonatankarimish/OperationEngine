package com.SixSense.threading;

import com.SixSense.config.ThreadingConfig;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class EngineThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup threadGroup;
    private final ThreadingConfig.ThreadingProperties threadingProperties;

    EngineThreadFactory(ThreadingConfig.ThreadingProperties threadingProperties) {
        SecurityManager securityManager = System.getSecurityManager();
        this.threadGroup = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.threadingProperties = threadingProperties;
    }

    /*Executors.DefaultThreadFactory() is sadly a private class :(
    * Therefore we are forced to implement a very similar class, with minor adjustments to apply our threadingProperties*/
    public Thread newThread(Runnable runnable) {
        Thread newThread = new Thread(this.threadGroup, runnable, this.threadingProperties.getThreadNamePrefix() + this.threadNumber.getAndIncrement(), 0L);
        if (newThread.isDaemon()) {
            newThread.setDaemon(false);
        }

        if (newThread.getPriority() != 5) {
            newThread.setPriority(5);
        }

        return newThread;
    }
}
