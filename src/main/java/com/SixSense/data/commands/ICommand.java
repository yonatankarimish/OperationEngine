package com.SixSense.data.commands;

import com.SixSense.data.outcomes.ExecutionCondition;
import com.SixSense.data.outcomes.ExpectedOutcome;
import com.SixSense.data.outcomes.LogicalCondition;
import com.SixSense.data.retention.VariableRetention;

import java.util.List;
import java.util.Map;

public interface ICommand {
    boolean isAlreadyExecuted();
    void setAlreadyExecuted(boolean alreadyExecuted);
    ICommand withAlreadyExecuted(boolean alreadyExecuted);

    List<ExecutionCondition> getExecutionConditions();

    AbstractCommand addExecutionCondition(ExecutionCondition executionCondition);

    AbstractCommand addExecutionConditions(List<ExecutionCondition> executionConditions);

    LogicalCondition getConditionAggregation();

    void setConditionAggregation(LogicalCondition conditionAggregation);

    AbstractCommand withConditionAggregation(LogicalCondition conditionAggregation);

    List<ExpectedOutcome> getExpectedOutcomes();

    ICommand addExpectedOutcome(ExpectedOutcome expectedOutcomes);

    ICommand addExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes);

    LogicalCondition getOutcomeAggregation();

    void setOutcomeAggregation(LogicalCondition outcomeAggregation);

    ICommand withOutcomeAggregation(LogicalCondition outcomeAggregation);

    String getAggregatedOutcomeMessage();

    void setAggregatedOutcomeMessage(String aggregatedOutcomeMessage);

    ICommand withAggregatedOutcomeMessage(String aggregatedOutcomeMessage);

    ICommand chainCommands(ICommand additional);

    Map<String, String> getDynamicFields();

    AbstractCommand addDynamicField(String key, String value);

    AbstractCommand addDynamicFields(Map<String, String> dynamicFields);

    VariableRetention getSaveTo();

    void setSaveTo(VariableRetention saveTo);

    ICommand withSaveTo(VariableRetention saveTo);

}
