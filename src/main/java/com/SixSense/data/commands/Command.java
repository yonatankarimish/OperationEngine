package com.SixSense.data.commands;

import com.SixSense.data.IDeepCloneable;
import com.SixSense.data.logic.ChannelType;
import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalExpression;
import com.SixSense.data.pipes.AbstractOutputPipe;
import com.SixSense.util.CommandUtils;

import java.util.*;

public class Command extends AbstractCommand implements ICommand, IDeepCloneable<Command> {
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour
    private String channelName;
    private String commandText;
    private int minimalSecondsToResponse;
    private int secondsToTimeout;

    private boolean requiresCleanup;
    private boolean useRawOutput;
    private LinkedHashSet<AbstractOutputPipe> outputPipes; // ordered set (i.e. no duplicate pipes)
    private LinkedHashSet<AbstractOutputPipe> retentionPipes; // ordered set (i.e. no duplicate pipes)

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for conditions and results only */
    public Command() {
        super();
        this.channelName = ChannelType.REMOTE.name();
        this.commandText = "";
        this.minimalSecondsToResponse = 0;
        this.secondsToTimeout = 10;

        this.requiresCleanup = true;
        this.useRawOutput = false;
        this.outputPipes = new LinkedHashSet<>();
        this.retentionPipes = new LinkedHashSet<>();
    }

    public Command(LogicalExpression<ExecutionCondition> executionCondition, LogicalExpression<ExpectedOutcome> expectedOutcome, String channelName, String commandText, int minimalSecondsToResponse, int secondsToTimeout, LinkedHashSet<AbstractOutputPipe> outputPipes, LinkedHashSet<AbstractOutputPipe> retentionPipes) {
        super(executionCondition, expectedOutcome);
        this.channelName = channelName;
        this.commandText = commandText;
        this.minimalSecondsToResponse = minimalSecondsToResponse;
        this.secondsToTimeout = secondsToTimeout;

        this.requiresCleanup = true;
        this.useRawOutput = false;
        this.outputPipes = outputPipes;
        this.retentionPipes = retentionPipes;
    }

    public ICommand chainCommands(ICommand additional){
        return CommandUtils.chainCommands(this, additional);
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannel(ChannelType commandType) {
        this.setChannelName(commandType.name());
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName.toUpperCase();
    }

    public Command withChannel(ChannelType commandType) {
        return this.withChannelName(commandType.name());
    }

    public Command withChannelName(String channelName) {
        this.channelName = channelName.toUpperCase();
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

    public boolean isRequiresCleanup() {
        return requiresCleanup;
    }

    public void setRequiresCleanup(boolean requiresCleanup) {
        this.requiresCleanup = requiresCleanup;
    }

    public Command withRequiresCleanup(boolean requiresCleanup) {
        this.requiresCleanup = requiresCleanup;
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

    public Set<AbstractOutputPipe> getRetentionPipes() {
        return retentionPipes;
    }

    public Command addRetentionPipe(AbstractOutputPipe retentionPipes) {
        this.retentionPipes.add(retentionPipes);
        return this;
    }

    public Command addRetentionPipes(Collection<AbstractOutputPipe> retentionPipes) {
        this.retentionPipes.addAll(retentionPipes);
        return this;
    }

    //Returns a new instance of the same command in its pristine state. That is - as if the new state was never executed
    @Override
    public Command deepClone(){
        return assignDefaults(new Command());
    }

    //Reverts the same command instance to it's pristine state.  That is - as if the same command was never executed
    @Override
    public Command reset(){
        return assignDefaults(this);
    }

    private Command assignDefaults(Command command){
        return (Command)command
                .withChannelName(this.channelName)
                .withCommandText(this.commandText)
                .withMinimalSecondsToResponse(this.minimalSecondsToResponse)
                .withSecondsToTimeout(this.secondsToTimeout)
                .withRequiresCleanup(this.requiresCleanup)
                .withUseRawOutput(this.useRawOutput)
                .addOutputPipes(this.outputPipes)
                .addRetentionPipes(this.retentionPipes)
                .withSuperCloneState(this);
    }

    @Override
    public String toString() {
        return "Command{" +
                "channelName=" + channelName +
                ", commandText='" + commandText + '\'' +
                ", minimalSecondsToResponse=" + minimalSecondsToResponse +
                ", secondsToTimeout=" + secondsToTimeout +
                ", requiresCleanup=" + requiresCleanup +
                ", useRawOutput=" + useRawOutput +
                ", outputPipes=" + outputPipes +
                ", retentionPipes=" + retentionPipes +
                ", " + super.superToString() +
                '}';
    }
}
