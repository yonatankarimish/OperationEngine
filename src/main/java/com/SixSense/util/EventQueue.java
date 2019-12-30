package com.SixSense.util;

import com.SixSense.data.events.AbstractEngineEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventQueue {
    private final Map<String, Deque<AbstractEngineEvent>> sessionEventQueue;

    public EventQueue(){
        this.sessionEventQueue = new ConcurrentHashMap<>();
    }

    public Collection<AbstractEngineEvent> getEventsForSession(String sessionId){
        Deque<AbstractEngineEvent> sessionEvents = this.sessionEventQueue.get(sessionId);
        if (sessionEvents == null) {
            return registerSession(sessionId);
        } else {
            return Collections.unmodifiableCollection(sessionEvents);
        }
    }

    public AbstractEngineEvent consumeNextEvent(String sessionId){
        Deque<AbstractEngineEvent> sessionEvents = this.sessionEventQueue.get(sessionId);
        if (sessionEvents == null) {
            registerSession(sessionId);
            return null;
        } else {
            return sessionEvents.pollFirst();
        }
    }

    //Should only push the new event if the session was manually registered (to avoid performance issues of automatically logging all sessions)
    public void pushIfRegistered(String sessionId, AbstractEngineEvent newEvent){
        Deque<AbstractEngineEvent> sessionEvents = this.sessionEventQueue.get(sessionId);
        if (sessionEvents != null) {
            sessionEvents.push(newEvent);
        }
    }

    public Collection<AbstractEngineEvent> registerSession(String sessionId){
        this.sessionEventQueue.putIfAbsent(sessionId, new ArrayDeque<>());
        return Collections.unmodifiableCollection(this.sessionEventQueue.get(sessionId));
    }

    public void unregisterSession(String sessionId){
        this.sessionEventQueue.remove(sessionId);
    }

    public void clear(){
        this.sessionEventQueue.clear();
    }
}