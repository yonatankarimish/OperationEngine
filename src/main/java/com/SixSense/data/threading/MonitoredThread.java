package com.SixSense.data.threading;

import com.SixSense.data.events.EngineEventType;
import com.SixSense.threading.IThreadMonitoingFactory;

//This class must extend the java.util.Thread class to allow returning MonitoredThread instances in library thread factories (tomcat, rabbitmq etc...)
public class MonitoredThread extends Thread {
    private final IThreadMonitoingFactory monitoringFactory;
    private final Thread thread;
    private EngineEventType currentLifecyclePhase;

    public MonitoredThread(IThreadMonitoingFactory monitoringFactory, Thread thread) {
        this.monitoringFactory = monitoringFactory;
        this.thread = thread;
        this.currentLifecyclePhase = EngineEventType.NotInSession;

        setName("Monitor(" + this.thread.getName() + ")");
    }

    @Override
    public void run() {
        this.monitoringFactory.watch(this);
        this.thread.run();
        this.monitoringFactory.unwatch(this);
    }

    public EngineEventType getCurrentLifecyclePhase() {
        return currentLifecyclePhase;
    }

    public void setCurrentLifecyclePhase(EngineEventType currentLifecyclePhase) {
        this.currentLifecyclePhase = currentLifecyclePhase;
    }
}