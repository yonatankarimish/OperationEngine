package com.SixSense.data.commands;

import com.SixSense.data.devices.Device;
import com.SixSense.data.logic.ChannelType;
import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalExpression;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Operation extends AbstractWorkflow implements ICommand, IWorkflow {
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour
    private String operationName;
    private ICommand executionBlock;
    private Set<String> channelNames;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for conditions, results and channel names */
    public Operation() {
        super();
        this.operationName = "";
        this.executionBlock = new Block();
        this.channelNames = new HashSet<>();
    }

    public Operation(LogicalExpression<ExecutionCondition> executionCondition, LogicalExpression<ExpectedOutcome> expectedOutcome, List<ParallelWorkflow> sequentialWorkflowUponSuccess, List<ParallelWorkflow> sequentialWorkflowUponFailure, String operationName, ICommand executionBlock, Set<String> channelNames) {
        super(executionCondition, expectedOutcome, sequentialWorkflowUponSuccess, sequentialWorkflowUponFailure);
        this.operationName = operationName;
        this.executionBlock = executionBlock;
        this.channelNames = channelNames;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Operation withOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    public ICommand getExecutionBlock() {
        return executionBlock;
    }

    public void setExecutionBlock(ICommand executionBlock) {
        this.executionBlock = executionBlock;
    }

    public Operation withExecutionBlock(ICommand executionBlock) {
        this.executionBlock = executionBlock;
        return this;
    }

    public Set<String> getChannelNames() {
        return Collections.unmodifiableSet(channelNames);
    }

    public Operation addChannel(ChannelType channelName) {
        return this.addChannelName(channelName.name());
    }

    public Operation addChannelName(String channelName) {
        this.channelNames.add(channelName.toUpperCase());
        return this;
    }

    public Operation addChannels(Set<ChannelType> channelNames) {
        return this.addChannelNames(channelNames.stream().map(ChannelType::name).collect(Collectors.toSet()));
    }

    public Operation addChannelNames(Set<String> channelNames) {
        this.channelNames.addAll(channelNames.stream().map(String::toUpperCase).collect(Collectors.toSet()));
        return this;
    }

    @Override
    public ICommand chainCommands(ICommand additional) {
        throw new UnsupportedOperationException("Not yet supported, but it should be...");
    }

    //Returns a new instance of the same operation in its pristine state. That is - as if the new state was never executed
    @Override
    public Operation deepClone(){
       return assignDefaults(new Operation());
    }

    //Reverts the same operation instance to it's pristine state.  That is - as if the same command was never executed
    @Override
    public Operation reset(){
        return assignDefaults(this);
    }

    private Operation assignDefaults(Operation operation){
        return (Operation)operation
                .withOperationName(this.operationName)
                .withExecutionBlock(this.executionBlock.deepClone())
                .addChannelNames(this.channelNames)
                .withSuperCloneState(this);
    }

    @Override
    public String toString() {
        return "Operation{" +
                "operationName='" + operationName + '\'' +
                ", executionBlock=" + executionBlock +
                ", channelNames=" + channelNames +
                ", " + super.superToString() +
                '}';
    }
}
