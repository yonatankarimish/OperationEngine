package com.sixsense.services;

import com.sixsense.model.events.AbstractEngineEvent;
import com.sixsense.model.events.EngineEventType;
import com.sixsense.model.events.IEngineEventHandler;
import com.sixsense.threading.ThreadingManager;
import com.sixsense.utillity.EventQueue;
import com.sixsense.utillity.ThreadingUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Service
/* Could have been called "EventManager" or something of that type,
 * but the main purpose of DiagnosticManager is to better understand engine internals, and not to randomly propagate events all around the place */
public class DiagnosticManager{
    private static final Logger logger = LogManager.getLogger(DiagnosticManager.class);
    private final LoggingManager loggingManager;
    private final ThreadingManager threadingManager;

    /* EnumMap<EngineEventType, HashSet<IEngineEventHandler>> allows for constant time lookup, register, unregister (O(1) time complexity)
     * while maintaining linear time emit (O(n) time complexity) where n denotes # of event listeners 
     * 
     * engineEventHandlers registers listeners for all session engine events. Designed to fire events asynchronously in separate threads.
     * futureEvents registers listeners that wait for lifecycle events for a single session. Threads block execution while waiting for these events.
     * eventQueue logs events emitted for sessions registered via the await() functions*/
    private final EnumMap<EngineEventType, HashSet<IEngineEventHandler>> engineEventHandlers = new EnumMap<>(EngineEventType.class);
    private final Map<String, EnumMap<EngineEventType, CompletableFuture<AbstractEngineEvent>>> futureEvents = new HashMap<>();
    private final EventQueue eventQueue = new EventQueue();

    @Autowired
    private DiagnosticManager(LoggingManager loggingManager, ThreadingManager threadingManager){
        this.loggingManager = loggingManager;
        this.threadingManager = threadingManager;

        for(EngineEventType eventType : EnumSet.allOf(EngineEventType.class)){
            engineEventHandlers.put(eventType, new HashSet<>());
        }
    }

    //Register an event handler to receive events from the diagnostic manager
    public void registerHandler(IEngineEventHandler eventHandler, EnumSet<EngineEventType> eventTypes){
        synchronized (this.engineEventHandlers) {
            for (EngineEventType eventType : eventTypes) {
                this.engineEventHandlers.get(eventType).add(eventHandler);
            }
        }
    }

    //Unregister an event handler so that no future events will be received from the diagnostic manager
    public void unregisterHandler(IEngineEventHandler eventHandler){
        synchronized (this.engineEventHandlers) {
            for(Collection<IEngineEventHandler> set : this.engineEventHandlers.values()){
                set.remove(eventHandler);
            }
        }
    }

    public void registerSession(String sessionId){
        this.eventQueue.registerSession(sessionId);
    }

    /*If an event of the required type has already occurred, return it immediately.
     * This method will register the relevant session with the event queue*/
    public Future<AbstractEngineEvent> await(String sessionId, EngineEventType eventType){
        synchronized (this.eventQueue) {
            Collection<AbstractEngineEvent> pastEvents = this.eventQueue.getEventsForSession(sessionId);
            for (AbstractEngineEvent pastEvent : pastEvents) {
                if (pastEvent.getEventType().equals(eventType)) {
                    return CompletableFuture.completedFuture(pastEvent);
                }
            }
        }

        /*If no event of the required type has occurred yet, register a future that will resolve when such an event occurs.
        * If such a future was already registered, return it instead*/
        return awaitFutureEvent(sessionId, eventType);
    }

    /* Consume events sequentially from the event queue.
     * If an event of type {eventType} has occurred, return with it immediately.
     * If an event of any other type has occurred, discard that event and continue consuming.*/
    public Future<AbstractEngineEvent> awaitAndConsume(String sessionId, EngineEventType eventType){
        synchronized (this.eventQueue) {
            AbstractEngineEvent nextEvent;
            do{
                nextEvent = eventQueue.consumeNextEvent(sessionId);
                if(nextEvent != null && nextEvent.getEventType().equals(eventType)){
                    return CompletableFuture.completedFuture(nextEvent);
                }
            } while(nextEvent != null);
        }

        /*If no event of the required type has occurred yet, register a future that will resolve when such an event occurs.
         * If such a future was already registered, return it instead*/
        return awaitFutureEvent(sessionId, eventType);
    }

    private Future<AbstractEngineEvent> awaitFutureEvent(String sessionId, EngineEventType eventType){
        EnumMap<EngineEventType, CompletableFuture<AbstractEngineEvent>> eventMap;
        synchronized (this.futureEvents){
            this.futureEvents.putIfAbsent(sessionId, new EnumMap<>(EngineEventType.class));
            eventMap = this.futureEvents.get(sessionId);
        }

        synchronized (eventMap) {
            eventMap.putIfAbsent(eventType, new CompletableFuture<>());
            return eventMap.get(eventType);
        }
    }

    public void emit(AbstractEngineEvent event){
        this.loggingManager.logEngineEvent(event);
        ThreadingUtils.updateLifecyclePhase(event.getEventType());

        if(event.getSession() != null) {
            String sessionId = event.getSession().getSessionShellId();
            synchronized (this.eventQueue) {
                /*To avoid any chance for leaking memory, it is not possible to await events at any point after the SessionClosed event*/
                if(event.getEventType().equals(EngineEventType.SessionClosed)){
                    synchronized (this.eventQueue){
                        this.eventQueue.unregisterSession(sessionId);
                    }
                }else {
                    this.eventQueue.pushIfRegistered(sessionId, event);
                }
            }

            EnumMap<EngineEventType, CompletableFuture<AbstractEngineEvent>> eventMap;
            synchronized (this.futureEvents) {
                /*But it is still possible to obtain their encapsulating futures, provided they were requested before the SessionClosed event*/
                eventMap = this.futureEvents.get(sessionId);
                if(event.getEventType().equals(EngineEventType.SessionClosed)){
                    this.futureEvents.remove(sessionId);
                }
            }

            if(eventMap != null){
                synchronized (eventMap) {
                    CompletableFuture<AbstractEngineEvent> futureEvent = eventMap.remove(event.getEventType());
                    if(futureEvent != null){
                        futureEvent.complete(event);
                    }
                }
            }
        }

        synchronized (this.engineEventHandlers){
            for (IEngineEventHandler iEngineEventHandler : this.engineEventHandlers.get(event.getEventType())) {
                try {
                    threadingManager.submit(() -> iEngineEventHandler.handleEngineEvent(event));
                } catch (Exception e) {
                    logger.error("Failed to emit task to event handler for engine event. Caused by: " + e.getMessage());
                }
            }
        }
    }

    public void clearDiagnosedSessions(){
        synchronized (this.futureEvents) {
            this.futureEvents.clear();
        }
        synchronized (this.eventQueue){
            this.eventQueue.clear();
        }
    }
}