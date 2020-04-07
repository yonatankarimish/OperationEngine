package com.SixSense.util;

import com.SixSense.data.events.EngineEventType;
import com.SixSense.data.threading.MonitoredThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadingUtils {
    private static final Logger logger = LogManager.getLogger(ThreadingUtils.class);

    public static void updateLifecyclePhase(EngineEventType currentLifecyclePhase){
        if(Thread.currentThread() instanceof MonitoredThread){
            MonitoredThread asMonitoredThread = (MonitoredThread)Thread.currentThread();
            asMonitoredThread.getCurrentThreadState().setCurrentLifecyclePhase(currentLifecyclePhase);
        }else{
            logger.warn(MessageLiterals.ThreadNotMonitored);
        }
    }

    public static void updateSessionId(String sessionId){
        if(Thread.currentThread() instanceof MonitoredThread){
            MonitoredThread asMonitoredThread = (MonitoredThread)Thread.currentThread();
            asMonitoredThread.getCurrentThreadState().setSessionId(sessionId);
        }else{
            logger.warn(MessageLiterals.ThreadNotMonitored);
        }
    }

    public static void updateOperationId(String operationId){
        if(Thread.currentThread() instanceof MonitoredThread){
            MonitoredThread asMonitoredThread = (MonitoredThread)Thread.currentThread();
            asMonitoredThread.getCurrentThreadState().setOperationId(operationId);
        }else{
            logger.warn(MessageLiterals.ThreadNotMonitored);
        }
    }

    public static void updateSessionAndOperationIds(String sessionId, String operationId){
        if(Thread.currentThread() instanceof MonitoredThread){
            MonitoredThread asMonitoredThread = (MonitoredThread)Thread.currentThread();
            asMonitoredThread.getCurrentThreadState().setSessionId(sessionId);
            asMonitoredThread.getCurrentThreadState().setOperationId(operationId);
        }else{
            logger.warn(MessageLiterals.ThreadNotMonitored);
        }
    }
}
