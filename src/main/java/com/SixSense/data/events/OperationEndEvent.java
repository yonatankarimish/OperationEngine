package com.SixSense.data.events;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.retention.OperationResult;
import com.SixSense.io.Session;

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
