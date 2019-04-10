package com.SixSense.data.commands;

import com.SixSense.data.devices.Device;
import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalCondition;
import com.SixSense.data.logic.WorkflowPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Operation extends AbstractWorkflow implements ICommand, IWorkflow {
    private String operationName;
    private Device device;
    private ICommand executionBlock;
    private List<ExpectedOutcome> expectedOutcomes;
    private LogicalCondition outcomeAggregation;

    public Operation() {
        super();
        this.operationName = "";
        this.device = new Device();
        this.expectedOutcomes = new ArrayList<>();
        this.outcomeAggregation = LogicalCondition.OR;
    }

    public Operation(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage, Set<WorkflowPolicy> workflowPolicies, List<IWorkflow> sequentialWorkflow, String operationName, Device device, ICommand executionBlock, List<ExpectedOutcome> expectedOutcomes1, LogicalCondition outcomeAggregation1) {
        super(executionConditions, conditionAggregation, expectedOutcomes, outcomeAggregation, aggregatedOutcomeMessage, workflowPolicies, sequentialWorkflow);
        this.operationName = operationName;
        this.device = device;
        this.executionBlock = executionBlock;
        this.expectedOutcomes = expectedOutcomes1;
        this.outcomeAggregation = outcomeAggregation1;
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
                ", expectedOutcomes=" + expectedOutcomes +
                ", outcomeAggregation=" + outcomeAggregation +
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
