package com.SixSense.data.events;

import com.SixSense.data.interfaces.IEquatable;
import com.SixSense.io.Session;

import java.util.Objects;

public abstract class AbstractEngineEvent implements IEquatable<AbstractEngineEvent> {
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

    @Override
    public boolean weakEquals(AbstractEngineEvent other){
        return this.eventType == other.eventType &&
            this.session == other.session;
    }

    @Override
    public boolean equals(Object other) {
        return this == other; //No two events are equal (even if they are of the same type on the same session, they were fired at different points in time)
    }

    @Override
    public boolean strongEquals(AbstractEngineEvent other){
        return this == other; //for the same reason as in equals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, session);
    }
}
