package com.SixSense.data.events;

import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.LogicalExpression;
import com.SixSense.io.Session;

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
