package com.SixSense.data.events;

import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.io.Session;

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
