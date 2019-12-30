package com.SixSense.data.events;

import com.SixSense.io.Session;

public abstract class AbstractEngineEvent {
    private EngineEventType eventType;
    private Session session;

    protected AbstractEngineEvent(EngineEventType eventType, Session session){
        this.eventType = eventType;
        this.session = session;
    }

    public EngineEventType getEventType(){
        return this.eventType;
    }

    public Session getSession() {
        return session;
    }
}
