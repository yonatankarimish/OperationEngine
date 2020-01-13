package com.SixSense.data.events;

import com.SixSense.io.Session;

public class SessionClosedEvent extends AbstractEngineEvent {
    public SessionClosedEvent(Session session) {
        super(EngineEventType.SessionClosed, session);
    }
}
