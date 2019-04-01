package com.SixSense.data.commands;

import com.SixSense.data.outcomes.CommandType;
import com.SixSense.data.outcomes.ExpectedOutcome;
import com.SixSense.data.outcomes.LogicalCondition;
import com.SixSense.util.CommandUtils;

import java.util.ArrayList;
import java.util.List;

public class Command extends AbstractCommand implements ICommand{
    private CommandType commandType;
    private String commandText;
    private int minimalSecondsToResponse;
    private int secondsToTimeout;

    public Command() {
        super();
        this.commandType = CommandType.REMOTE;
        this.commandText = "";
        this.minimalSecondsToResponse = 0;
        this.secondsToTimeout = 10;
        this.expectedOutcomes = new ArrayList<>();
        this.outcomeAggregation = LogicalCondition.OR;
        this.aggregatedOutcomeMessage = "";
    }

    public Command(CommandType commandType, String commandText, int minimalTimeToResponse, int secondsToTimeout, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage) {
        super(expectedOutcomes, outcomeAggregation, aggregatedOutcomeMessage);
        this.commandType = commandType;
        this.commandText = commandText;
        this.minimalSecondsToResponse = minimalTimeToResponse;
        this.secondsToTimeout = secondsToTimeout;
    }

    public ICommand chainCommands(ICommand additional){
        return CommandUtils.chainCommands(this, additional);
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
    public String toString() {
        return "Command{" +
                "commandType=" + commandType +
                ", commandText='" + commandText + '\'' +
                ", minimalSecondsToResponse=" + minimalSecondsToResponse +
                ", secondsToTimeout=" + secondsToTimeout +
                ", alreadyExecuted=" + alreadyExecuted +
                ", expectedOutcomes=" + expectedOutcomes +
                ", outcomeAggregation=" + outcomeAggregation +
                ", aggregatedOutcomeMessage='" + aggregatedOutcomeMessage + '\'' +
                ", dynamicFields=" + dynamicFields +
                ", saveTo=" + saveTo +
                '}';
    }
}
