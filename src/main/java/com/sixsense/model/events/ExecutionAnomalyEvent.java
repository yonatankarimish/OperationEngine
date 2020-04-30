package com.sixsense.model.events;

import com.sixsense.model.logic.ExpressionResult;
import com.sixsense.io.Session;

public class ExecutionAnomalyEvent extends AbstractEngineEvent {
    private ExpressionResult result;

    public ExecutionAnomalyEvent(Session session, ExpressionResult result) {
        super(EngineEventType.ExecutionAnomaly, session);
        this.result = result;
    }

    public ExpressionResult getResult() {
        return result;
    }
}
