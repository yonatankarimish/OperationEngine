package com.SixSense.data.commands;

import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.data.Outcomes.LogicalCondition;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCommand implements ICommand{
    protected List<ExpectedOutcome> expectedOutcomes;
    protected LogicalCondition outcomeAggregation;
    protected String aggregatedOutcomeMessage;

    public AbstractCommand(){
        this.expectedOutcomes = new ArrayList<>();
        this.outcomeAggregation = LogicalCondition.OR;
        this.aggregatedOutcomeMessage = "";
    }

    public AbstractCommand(List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage) {
        this.expectedOutcomes = expectedOutcomes;
        this.outcomeAggregation = outcomeAggregation;
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;
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

    public void setOutcomeAggregation(LogicalCondition outcomeAggregation) {
        this.outcomeAggregation = outcomeAggregation;
    }

    public AbstractCommand withOutcomeAggregation(LogicalCondition outcomeAggregation) {
        this.outcomeAggregation = outcomeAggregation;
        return this;
    }

    @Override
    public String getAggregatedOutcomeMessage() {
        return aggregatedOutcomeMessage;
    }

    public void setAggregatedOutcomeMessage(String aggregatedOutcomeMessage) {
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;
    }

    public AbstractCommand withAggregatedOutcomeMessage(String aggregatedOutcomeMessage) {
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;
        return this;
    }
}
