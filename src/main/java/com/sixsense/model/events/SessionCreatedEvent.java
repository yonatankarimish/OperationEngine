package com.sixsense.model.events;

import com.sixsense.io.Session;

public class SessionCreatedEvent extends AbstractEngineEvent {
    public SessionCreatedEvent(Session session) {
        super(EngineEventType.SessionCreated, session);
    }
}
