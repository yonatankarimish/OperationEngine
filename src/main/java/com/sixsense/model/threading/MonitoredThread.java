package com.sixsense.model.threading;

import com.sixsense.threading.IThreadMonitoingFactory;

//This class must extend the java.util.Thread class to allow returning MonitoredThread instances in library thread factories (tomcat, rabbitmq etc...)
public class MonitoredThread extends Thread {
    private final IThreadMonitoingFactory monitoringFactory;
    private final Thread thread;
    private final MonitoredThreadState currentThreadState;

    public MonitoredThread(IThreadMonitoingFactory monitoringFactory, Thread thread) {
        this.monitoringFactory = monitoringFactory;
        this.thread = thread;
        this.currentThreadState = new MonitoredThreadState();

        setName("Monitor(" + this.thread.getName() + ")");
    }

    public MonitoredThreadState getCurrentThreadState() {
        return currentThreadState;
    }

    @Override
    public void run() {
        this.monitoringFactory.watch(this);
        this.thread.run();
        this.monitoringFactory.unwatch(this);
    }
}