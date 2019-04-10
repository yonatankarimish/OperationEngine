package com.SixSense.data.commands;

import com.SixSense.data.logic.CommandType;
import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalCondition;
import com.SixSense.data.pipes.AbstractOutputPipe;
import com.SixSense.util.CommandUtils;

import java.util.*;

public class Command extends AbstractCommand implements ICommand{
    private CommandType commandType;
    private String commandText;
    private int minimalSecondsToResponse;
    private int secondsToTimeout;

    private boolean useRawOutput;
    private LinkedHashSet<AbstractOutputPipe> outputPipes; // ordered set (i.e. no duplicate pipes)

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for conditions and results only */
    public Command() {
        super();
        this.commandType = CommandType.REMOTE;
        this.commandText = "";
        this.minimalSecondsToResponse = 0;
        this.secondsToTimeout = 10;
        this.expectedOutcomes = new ArrayList<>();
        this.outcomeAggregation = LogicalCondition.OR;
        this.aggregatedOutcomeMessage = "";

        this.useRawOutput = false;
        this.outputPipes = new LinkedHashSet<>();
    }

    public Command(CommandType commandType, String commandText, int minimalSecondsToResponse, int secondsToTimeout, List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage, LinkedHashSet<AbstractOutputPipe> outputPipes) {
        super(executionConditions, conditionAggregation, expectedOutcomes, outcomeAggregation, aggregatedOutcomeMessage);
        this.commandType = commandType;
        this.commandText = commandText;
        this.minimalSecondsToResponse = minimalSecondsToResponse;
        this.secondsToTimeout = secondsToTimeout;

        this.useRawOutput = false;
        this.outputPipes = outputPipes;
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

    public boolean isUseRawOutput() {
        return useRawOutput;
    }

    public void setUseRawOutput(boolean useRawOutput) {
        this.useRawOutput = useRawOutput;
    }

    public Command withUseRawOutput(boolean useRawOutput) {
        this.useRawOutput = useRawOutput;
        return this;
    }

    public Set<AbstractOutputPipe> getOutputPipes() {
        return outputPipes;
    }

    public Command addOutputPipe(AbstractOutputPipe outputPipe) {
        this.outputPipes.add(outputPipe);
        return this;
    }

    public Command addOutputPipes(Collection<AbstractOutputPipe> outputPipes) {
        this.outputPipes.addAll(outputPipes);
        return this;
    }

    @Override
    public String toString() {
        return "Command{" +
                "commandType=" + commandType +
                ", commandText='" + commandText + '\'' +
                ", minimalSecondsToResponse=" + minimalSecondsToResponse +
                ", secondsToTimeout=" + secondsToTimeout +
                ", useRawOutput=" + useRawOutput +
                ", outputPipes=" + outputPipes +
                ", alreadyExecuted=" + alreadyExecuted +
                ", executionConditions=" + executionConditions +
                ", conditionAggregation=" + conditionAggregation +
                ", expectedOutcomes=" + expectedOutcomes +
                ", outcomeAggregation=" + outcomeAggregation +
                ", aggregatedOutcomeMessage='" + aggregatedOutcomeMessage + '\'' +
                ", dynamicFields=" + dynamicFields +
                ", saveTo=" + saveTo +
                '}';
    }
}
