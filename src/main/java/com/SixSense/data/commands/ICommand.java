package com.SixSense.data.commands;

import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.data.Outcomes.LogicalCondition;

import java.util.List;

public interface ICommand {
    List<ExpectedOutcome> getExpectedOutcomes();
    void setExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes);
    ICommand withExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes);
    LogicalCondition getOutcomeAggregation();
    String getAggregatedOutcomeMessage();
    ICommand chainCommands(ICommand additional);
}
