package com.SixSense.data.commands;

import com.SixSense.data.devices.Device;
import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalCondition;

import java.util.List;

public class Operation extends AbstractWorkflow implements ICommand, IWorkflow {
    private String operationName;
    private Device device;
    private ICommand executionBlock;

    public Operation() {
        super();
        this.operationName = "";
        this.device = new Device();
        this.executionBlock = new Block();
    }

    public Operation(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage, List<ParallelWorkflow> sequentialWorkflowUponSuccess, List<ParallelWorkflow> sequentialWorkflowUponFailure, String operationName, Device device, ICommand executionBlock) {
        super(executionConditions, conditionAggregation, expectedOutcomes, outcomeAggregation, aggregatedOutcomeMessage, sequentialWorkflowUponSuccess, sequentialWorkflowUponFailure);
        this.operationName = operationName;
        this.device = device;
        this.executionBlock = executionBlock;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Operation withDevice(Device device) {
        this.device = device;
        return this;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getFullOperationName() {
        return device.getVpv().getName() + " " + operationName;
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
                .withDevice(this.device.deepClone())
                .withExecutionBlock(this.executionBlock.deepClone())
                .withSuperCloneState(this);
    }

    @Override
    public String toString() {
        return "Operation{" +
                "operationName='" + operationName + '\'' +
                ", device=" + device +
                ", executionBlock=" + executionBlock +
                ", " + super.superToString() +
                '}';
    }
}
