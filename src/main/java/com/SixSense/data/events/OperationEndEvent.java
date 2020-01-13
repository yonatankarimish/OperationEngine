package com.SixSense.data.events;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.io.Session;

public class OperationEndEvent extends AbstractEngineEvent {
    private Operation operation;
    private ExpressionResult result;

    public OperationEndEvent(Session session, Operation operation, ExpressionResult result) {
        super(EngineEventType.OperationEnd, session);
        this.operation = operation;
        this.result = result;
    }

    public Operation getOperation() {
        return operation;
    }

    public ExpressionResult getResult() {
        return result;
    }
}
