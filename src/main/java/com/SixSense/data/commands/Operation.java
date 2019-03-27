package com.SixSense.data.commands;

import com.SixSense.data.Outcomes.ExpectedOutcome;

import java.util.List;

public class Operation implements ICommand {
    private String VPV;
    private String operationName;
    private ICommand executionBlock;
    private List<ExpectedOutcome> expectedOutcomes;

    public Operation() {
    }

    public Operation(String VPV, String operationName, ICommand executionBlock, List<ExpectedOutcome> expectedOutcomes) {
        this.VPV = VPV;
        this.operationName = operationName;
        this.executionBlock = executionBlock;
        this.expectedOutcomes = expectedOutcomes;
    }

    public String getVPV() {
        return VPV;
    }

    public void setVPV(String VPV) {
        this.VPV = VPV;
    }

    public Operation withVPV(String VPV) {
        this.VPV = VPV;
        return this;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getFullOperationName() {
        return VPV + " " + operationName;
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
    public List<ExpectedOutcome> getExpectedOutcomes() {
        return null;
    }

    @Override
    public ICommand chainCommands(ICommand additional) {
        throw new UnsupportedOperationException("Not yet supported, but it should be...");
    }

    public void setExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
    }

    public Operation withExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
        return this;
    }

    @Override
    public String toString() {
        return "Operation{" +
                "VPV='" + VPV + '\'' +
                ", operationName='" + operationName + '\'' +
                ", executionBlock=" + executionBlock +
                ", expectedOutcomes=" + expectedOutcomes +
                '}';
    }
}
