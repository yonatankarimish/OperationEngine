package com.sixsense.threading;

import com.sixsense.model.threading.MonitoredThread;

import java.util.Set;

public interface IThreadMonitoingFactory {
    Thread newThread(Runnable runnable); //might be redundant, but forces all implementing classes to implement this method, regardless of any other interfaces it might implememt
    Set<MonitoredThread> getMonitoredThreads();
    boolean watch(MonitoredThread monitoredThread);
    boolean unwatch(MonitoredThread monitoredThread);
}
