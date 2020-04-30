package com.sixsense.model.threading;

import com.sixsense.model.events.EngineEventType;

public class MonitoredThreadState {
    private static final String NotInSession = "no-running-session";

    private EngineEventType currentLifecyclePhase;
    private String sessionId;
    private String operationId;

    MonitoredThreadState() {
        resetState();
    }

    public EngineEventType getCurrentLifecyclePhase() {
        return currentLifecyclePhase;
    }

    public void setCurrentLifecyclePhase(EngineEventType currentLifecyclePhase) {
        this.currentLifecyclePhase = currentLifecyclePhase;
    }

    public MonitoredThreadState withCurrentLifecyclePhase(EngineEventType currentLifecyclePhase) {
        this.currentLifecyclePhase = currentLifecyclePhase;
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public MonitoredThreadState withSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public MonitoredThreadState withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public MonitoredThreadState resetState(){
        this.currentLifecyclePhase = EngineEventType.NotInSession;
        this.sessionId = NotInSession;
        this.operationId = NotInSession;
        return this;
    }

    @Override
    public String toString() {
        return "MonitoredThreadState{" +
                "currentLifecyclePhase=" + currentLifecyclePhase +
                ", sessionId='" + sessionId + '\'' +
                ", operationId='" + operationId + '\'' +
                '}';
    }
}
