package com.sixsense.model.events;

import com.sixsense.io.Session;

public class SessionClosedEvent extends AbstractEngineEvent {
    public SessionClosedEvent(Session session) {
        super(EngineEventType.SessionClosed, session);
    }
}
