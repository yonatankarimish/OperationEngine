package com.SixSense.data.events;

import com.SixSense.io.Session;

public class SessionCreatedEvent extends AbstractEngineEvent {
    public SessionCreatedEvent(Session session) {
        super(EngineEventType.SessionCreated, session);
    }
}
