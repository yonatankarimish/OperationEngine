package com.SixSense.data.events;

import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalExpression;
import com.SixSense.io.Session;

public class OutcomeEvaluationEvent extends AbstractEngineEvent{
    private String result;
    private LogicalExpression<ExpectedOutcome> expectedOutcome;

    public OutcomeEvaluationEvent(Session session, String result, LogicalExpression<ExpectedOutcome> expectedOutcome) {
        super(EngineEventType.OutcomeEvaluation, session);
        this.result = result;
        this.expectedOutcome = expectedOutcome;
    }

    public String getResult() {
        return result;
    }

    public LogicalExpression<ExpectedOutcome> getExpectedOutcome() {
        return expectedOutcome;
    }
}
