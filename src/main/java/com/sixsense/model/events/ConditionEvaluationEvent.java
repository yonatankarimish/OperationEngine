package com.sixsense.model.events;

import com.sixsense.model.logic.ExecutionCondition;
import com.sixsense.model.logic.LogicalExpression;
import com.sixsense.io.Session;

public class ConditionEvaluationEvent extends AbstractEngineEvent{
    private LogicalExpression<ExecutionCondition> condition;

    public ConditionEvaluationEvent(Session session, LogicalExpression<ExecutionCondition> condition) {
        super(EngineEventType.ConditionEvaluation, session);
        this.condition = condition;
    }

    public LogicalExpression<ExecutionCondition> getCondition() {
        return condition;
    }
}
