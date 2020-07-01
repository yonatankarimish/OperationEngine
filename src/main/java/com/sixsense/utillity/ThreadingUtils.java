package com.sixsense.utillity;

import com.sixsense.model.events.EngineEventType;
import com.sixsense.model.threading.MonitoredThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadingUtils {
    private static final Logger logger = LogManager.getLogger(ThreadingUtils.class);

    private ThreadingUtils(){
        /*Empty private constructor - no instances of this class should be created */
    }

    public static void updateLifecyclePhase(EngineEventType currentLifecyclePhase){
        if(Thread.currentThread() instanceof MonitoredThread){
            MonitoredThread asMonitoredThread = (MonitoredThread)Thread.currentThread();
            asMonitoredThread.getCurrentThreadState().setCurrentLifecyclePhase(currentLifecyclePhase);
        }else{
            logger.warn(Literals.ThreadNotMonitored);
        }
    }

    public static void updateSessionId(String sessionId){
        if(Thread.currentThread() instanceof MonitoredThread){
            MonitoredThread asMonitoredThread = (MonitoredThread)Thread.currentThread();
            asMonitoredThread.getCurrentThreadState().setSessionId(sessionId);
        }else{
            logger.warn(Literals.ThreadNotMonitored);
        }
    }

    public static void updateOperationId(String operationId){
        if(Thread.currentThread() instanceof MonitoredThread){
            MonitoredThread asMonitoredThread = (MonitoredThread)Thread.currentThread();
            asMonitoredThread.getCurrentThreadState().setOperationId(operationId);
        }else{
            logger.warn(Literals.ThreadNotMonitored);
        }
    }

    public static void updateSessionAndOperationIds(String sessionId, String operationId){
        if(Thread.currentThread() instanceof MonitoredThread){
            MonitoredThread asMonitoredThread = (MonitoredThread)Thread.currentThread();
            asMonitoredThread.getCurrentThreadState().setSessionId(sessionId);
            asMonitoredThread.getCurrentThreadState().setOperationId(operationId);
        }else{
            logger.warn(Literals.ThreadNotMonitored);
        }
    }
}
