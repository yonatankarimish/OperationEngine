package com.SixSense.threading;

import com.SixSense.config.ThreadingConfig;
import com.SixSense.data.events.EngineEventType;
import com.SixSense.data.threading.MonitoredThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class EngineThreadFactory implements ThreadFactory, IThreadMonitoingFactory {
    private static final Logger logger = LogManager.getLogger(EngineThreadFactory.class);
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup threadGroup;
    private final ThreadingConfig.ThreadingProperties threadingProperties;
    private final Set<MonitoredThread> monitoredThreads;

    EngineThreadFactory(ThreadingConfig.ThreadingProperties threadingProperties) {
        SecurityManager securityManager = System.getSecurityManager();
        this.threadGroup = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.threadingProperties = threadingProperties;
        this.monitoredThreads = Collections.synchronizedSet(new HashSet<>());
    }

    /*Executors.DefaultThreadFactory() is sadly a private class :(
    * Therefore we are forced to implement a very similar class, with minor adjustments to apply our threadingProperties*/
    @Override
    public Thread newThread(Runnable runnable) {
        Thread newThread = new Thread(this.threadGroup, runnable, this.threadingProperties.getThreadNamePrefix() + this.threadNumber.getAndIncrement(), 0L);
        if (newThread.isDaemon()) {
            newThread.setDaemon(false);
        }

        if (newThread.getPriority() != 5) {
            newThread.setPriority(5);
        }

        logger.debug("Created new thread " + newThread.getName());
        return new MonitoredThread(this, newThread);
    }

    @Override
    public Set<MonitoredThread> getMonitoredThreads(){
        return Collections.unmodifiableSet(this.monitoredThreads);
    }

    @Override
    public boolean watch(MonitoredThread monitoredThread) {
        logger.debug("Watching thread " + monitoredThread.getName());
        return this.monitoredThreads.add(monitoredThread);
    }

    @Override
    public boolean unwatch(MonitoredThread monitoredThread) {
        logger.debug("Unwatching thread " + monitoredThread.getName());
        monitoredThread.setCurrentLifecyclePhase(EngineEventType.NotInSession);
        return this.monitoredThreads.remove(monitoredThread);
    }
}
