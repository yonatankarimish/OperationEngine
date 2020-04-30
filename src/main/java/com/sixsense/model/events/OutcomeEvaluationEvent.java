package com.sixsense.model.events;

import com.sixsense.model.logic.ExpectedOutcome;
import com.sixsense.model.logic.LogicalExpression;
import com.sixsense.io.Session;

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
