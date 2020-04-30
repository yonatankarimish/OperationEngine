package com.sixsense.model.commands;

import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.interfaces.IEquatable;
import com.sixsense.model.logic.ChannelType;
import com.sixsense.model.logic.ExecutionCondition;
import com.sixsense.model.logic.ExpectedOutcome;
import com.sixsense.model.logic.LogicalExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Operation extends AbstractWorkflow implements ICommand, IWorkflow, IDeepCloneable<Operation>, IEquatable<Operation> {
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
    public boolean weakEquals(Operation other) {
        return super.weakEquals(other) && this.equals(other);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || this.getClass() != other.getClass()) {
            return false;
        } else {
            Operation otherAsOperation = (Operation) other;
            return super.equals(otherAsOperation) && this.equals(otherAsOperation);
        }
    }

    public boolean equals(Operation other) {
        return this.operationName.equals(other.operationName) &&
            this.executionBlock.equals(other.executionBlock) &&
            this.channelNames.equals(other.channelNames);
    }

    @Override
    public boolean strongEquals(Operation other) {
        return super.strongEquals(other) && this.equals(other);
    }

    @Override
    public int hashCode() {
        Stream<Object> childStream = Arrays.stream(new Object[]{operationName, executionBlock, channelNames});
        Stream<Object> superStream = Arrays.stream(superMembers());

        Object[] mergedMembers = Stream.concat(superStream, childStream).toArray();
        return Arrays.hashCode(mergedMembers);
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
