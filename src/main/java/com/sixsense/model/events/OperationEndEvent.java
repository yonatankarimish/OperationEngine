package com.sixsense.model.events;

import com.sixsense.model.commands.Operation;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.io.Session;

public class OperationEndEvent extends AbstractEngineEvent {
    private Operation operation;
    private OperationResult result;

    public OperationEndEvent(Session session, Operation operation, OperationResult result) {
        super(EngineEventType.OperationEnd, session);
        this.operation = operation;
        this.result = result;
    }

    public Operation getOperation() {
        return operation;
    }

    public OperationResult getResult() {
        return result;
    }
}
