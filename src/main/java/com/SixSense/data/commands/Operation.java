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

    @Override
    public String toString() {
        return "Operation{" +
                "operationName='" + operationName + '\'' +
                ", device=" + device +
                ", executionBlock=" + executionBlock +
                ", uuid=" + uuid +
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
