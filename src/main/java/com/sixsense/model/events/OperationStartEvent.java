package com.sixsense.model.events;

import com.sixsense.model.commands.Operation;
import com.sixsense.io.Session;

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
