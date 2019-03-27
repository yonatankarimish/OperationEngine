package com.SixSense.data.commands;

import com.SixSense.data.Outcomes.ExpectedOutcome;

import java.util.List;

public interface ICommand {
    List<ExpectedOutcome> getExpectedOutcomes();
    void setExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes);
    ICommand withExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes);
    ICommand chainCommands(ICommand additional);
}
