package com.SixSense.data.commands;

import com.SixSense.data.outcomes.ExecutionCondition;
import com.SixSense.data.outcomes.ExpectedOutcome;
import com.SixSense.data.outcomes.LogicalCondition;
import com.SixSense.data.retention.VariableRetention;

import java.util.*;

public abstract class AbstractCommand implements ICommand{
    protected boolean alreadyExecuted;

    protected List<ExecutionCondition> executionConditions;
    protected LogicalCondition conditionAggregation;

    protected List<ExpectedOutcome> expectedOutcomes;
    protected LogicalCondition outcomeAggregation;
    protected String aggregatedOutcomeMessage;

    protected Map<String, String> dynamicFields;
    protected VariableRetention saveTo;

    /*Try not to pollute with additional constructors
    * The empty constructor is for using the 'with' design pattern
    * The parameterized constructor is for conditions and results only */
    public AbstractCommand(){
        this.alreadyExecuted = false;

        this.executionConditions = new ArrayList<>();
        this.conditionAggregation = LogicalCondition.OR;

        this.expectedOutcomes = new ArrayList<>();
        this.outcomeAggregation = LogicalCondition.OR;
        this.aggregatedOutcomeMessage = "";

        this.dynamicFields = new HashMap<>();
        this.saveTo = new VariableRetention();
    }

    public AbstractCommand(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage) {
        this.alreadyExecuted = false;

        this.executionConditions = executionConditions;
        this.conditionAggregation = conditionAggregation;

        this.expectedOutcomes = expectedOutcomes;
        this.outcomeAggregation = outcomeAggregation;
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;

        this.dynamicFields = new HashMap<>();
        this.saveTo = new VariableRetention();
    }

    @Override
    public boolean isAlreadyExecuted() {
        return alreadyExecuted;
    }

    @Override
    public void setAlreadyExecuted(boolean alreadyExecuted) {
        this.alreadyExecuted = alreadyExecuted;
    }

    @Override
    public AbstractCommand withAlreadyExecuted(boolean hasBeenExecuted) {
        this.alreadyExecuted = hasBeenExecuted;
        return this;
    }

    @Override
    public List<ExecutionCondition> getExecutionConditions() {
        return executionConditions;
    }

    @Override
    public void setExecutionConditions(List<ExecutionCondition> executionConditions) {
        this.executionConditions = executionConditions;
    }

    @Override
    public AbstractCommand withExecutionConditions(List<ExecutionCondition> executionConditions) {
        this.executionConditions = executionConditions;
        return this;
    }

    @Override
    public LogicalCondition getConditionAggregation() {
        return conditionAggregation;
    }

    @Override
    public void setConditionAggregation(LogicalCondition conditionAggregation) {
        this.conditionAggregation = conditionAggregation;
    }

    @Override
    public AbstractCommand withConditionAggregation(LogicalCondition conditionAggregation) {
        this.conditionAggregation = conditionAggregation;
        return this;
    }

    @Override
    public List<ExpectedOutcome> getExpectedOutcomes() {
        return expectedOutcomes;
    }

    @Override
    public void setExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
    }

    @Override
    public AbstractCommand withExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
        return this;
    }

    @Override
    public LogicalCondition getOutcomeAggregation() {
        return outcomeAggregation;
    }

    @Override
    public void setOutcomeAggregation(LogicalCondition outcomeAggregation) {
        this.outcomeAggregation = outcomeAggregation;
    }

    @Override
    public AbstractCommand withOutcomeAggregation(LogicalCondition outcomeAggregation) {
        this.outcomeAggregation = outcomeAggregation;
        return this;
    }

    @Override
    public String getAggregatedOutcomeMessage() {
        return aggregatedOutcomeMessage;
    }

    @Override
    public void setAggregatedOutcomeMessage(String aggregatedOutcomeMessage) {
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;
    }

    @Override
    public AbstractCommand withAggregatedOutcomeMessage(String aggregatedOutcomeMessage) {
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;
        return this;
    }

    @Override
    public Map<String, String> getDynamicFields() {
        return Collections.unmodifiableMap(this.dynamicFields);
    }

    @Override
    public AbstractCommand addDynamicFields(Map<String, String> dynamicFields) {
        this.dynamicFields.putAll(dynamicFields);
        return this;
    }

    @Override
    public VariableRetention getSaveTo() {
        return saveTo;
    }

    @Override
    public void setSaveTo(VariableRetention saveTo) {
        this.saveTo = saveTo;
    }

    @Override
    public AbstractCommand withSaveTo(VariableRetention saveTo) {
        this.saveTo = saveTo;
        return this;
    }
}
