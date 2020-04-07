package com.SixSense.api.http.overrides;

import com.SixSense.data.threading.MonitoredThread;
import com.SixSense.threading.IThreadMonitoingFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.threads.TaskThreadFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HTTPThreadFactory extends TaskThreadFactory implements IThreadMonitoingFactory {
    private static final Logger logger = LogManager.getLogger(HTTPThreadFactory.class);
    private final Set<MonitoredThread> monitoredThreads;

    HTTPThreadFactory(String namePrefix, boolean daemon, int priority) {
        super(namePrefix, daemon, priority);
        this.monitoredThreads = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public Thread newThread(Runnable runnable){
        Thread newThread = super.newThread(runnable);
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
        monitoredThread.getCurrentThreadState().resetState();
        return this.monitoredThreads.remove(monitoredThread);
    }
}
