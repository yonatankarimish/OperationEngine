package com.SixSense.data.events;

import com.SixSense.data.commands.Operation;
import com.SixSense.io.Session;

public class OperationStartEvent extends AbstractEngineEvent {
    private Operation operation;

    public OperationStartEvent(Session session, Operation operation) {
        super(EngineEventType.OperationStart, session);
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }
}
