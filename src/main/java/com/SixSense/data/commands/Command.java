package com.SixSense.data.commands;

import com.SixSense.data.Outcomes.CommandType;
import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.util.BlockUtils;

import java.util.List;

public class Command implements ICommand{
    private CommandType commandType;
    private String commandText;
    private int minimalSecondsToResponse;
    private int secondsToTimeout;
    private List<ExpectedOutcome> expectedOutcomes;

    public Command() {
    }

    public Command(CommandType commandType, String commandText, int minimalTimeToResponse, int secondsToTimeout, List<ExpectedOutcome> expectedOutcomes) {
        this.commandType = commandType;
        this.commandText = commandText;
        this.minimalSecondsToResponse = minimalTimeToResponse;
        this.secondsToTimeout = secondsToTimeout;
        this.expectedOutcomes = expectedOutcomes;
    }

    public ICommand chainCommands(ICommand additional){
        return BlockUtils.chainCommands(this, additional);
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public Command withCommandType(CommandType commandType) {
        this.commandType = commandType;
        return this;
    }

    public String getCommandText() {
        return commandText;
    }

    public void setCommandText(String commandText) {
        this.commandText = commandText;
    }

    public Command withCommandText(String commandText) {
        this.commandText = commandText;
        return this;
    }

    public int getMinimalSecondsToResponse() {
        return minimalSecondsToResponse;
    }

    public void setMinimalSecondsToResponse(int minimalSecondsToResponse) {
        this.minimalSecondsToResponse = minimalSecondsToResponse;
    }

    public Command withMinimalSecondsToResponse(int minimalTimeToResponse) {
        this.minimalSecondsToResponse = minimalTimeToResponse;
        return this;
    }

    public int getSecondsToTimeout() {
        return secondsToTimeout;
    }

    public void setSecondsToTimeout(int secondsToTimeout) {
        this.secondsToTimeout = secondsToTimeout;
    }

    public Command withSecondsToTimeout(int timout) {
        this.secondsToTimeout = timout;
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
    public Command withExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
        return this;
    }

    @Override
    public String toString() {
        return "Command{" +
                "commandType=" + commandType +
                ", commandText='" + commandText + '\'' +
                ", minimalSecondsToResponse=" + minimalSecondsToResponse +
                ", secondsToTimeout=" + secondsToTimeout +
                ", expectedOutcomes=" + expectedOutcomes +
                '}';
    }
}
