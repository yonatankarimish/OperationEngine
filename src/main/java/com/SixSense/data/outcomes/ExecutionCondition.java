package com.SixSense.data.outcomes;

import java.util.Objects;

public class ExecutionCondition implements IFlowConnector {
    private String variable;
    private BinaryRelation binaryRelation;
    private String expectedValue;
    private boolean resolved;

    //The default Execution condition wills search for the empty string in any session field, therefore always returning CommandResult.SUCCESS.
    public ExecutionCondition(){
        this.variable = "";
        this.binaryRelation = BinaryRelation.CONTAINS;
        this.expectedValue = "";
        this.resolved = false;
    }

    public ExecutionCondition(String variable, BinaryRelation binaryRelation, String expectedValue) {
        this.variable = variable;
        this.binaryRelation = binaryRelation;
        this.expectedValue = expectedValue;
        this.resolved = false;
    }

    public ExecutionCondition (ExecutionCondition clone){
        this.variable = clone.variable;
        this.binaryRelation = clone.binaryRelation;
        this.expectedValue = clone.expectedValue;
        this.resolved = clone.resolved;
    }

    public static ExecutionCondition matchingSuccess(){
        return new ExecutionCondition()
                .withBinaryRelation(BinaryRelation.NONE)
                .withResolved(true);
    }

    public static ExecutionCondition matchingFailure(){
        return new ExecutionCondition().withBinaryRelation(BinaryRelation.NONE);
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public ExecutionCondition withVariable(String variable) {
        this.variable = variable;
        return this;
    }

    @Override
    public BinaryRelation getBinaryRelation() {
        return binaryRelation;
    }

    public void setBinaryRelation(BinaryRelation binaryRelation) {
        this.binaryRelation = binaryRelation;
    }

    public ExecutionCondition withBinaryRelation(BinaryRelation binaryRelation) {
        this.binaryRelation = binaryRelation;
        return this;
    }

    @Override
    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public ExecutionCondition withExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
        return this;
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    @Override
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public ExecutionCondition withResolved(boolean resolved) {
        this.resolved = resolved;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        else if (other == null || getClass() != other.getClass()) {
            return false;
        }else {
            ExecutionCondition otherCondition = (ExecutionCondition) other;
            return resolved == otherCondition.resolved &&
                    Objects.equals(variable, otherCondition.variable) &&
                    binaryRelation == otherCondition.binaryRelation &&
                    Objects.equals(expectedValue, otherCondition.expectedValue);
        }
    }

    @Override
    public String toString() {
        return "ExecutionCondition{" +
                "variable='" + variable + '\'' +
                ", binaryRelation=" + binaryRelation +
                ", expectedValue='" + expectedValue + '\'' +
                ", resolved=" + resolved +
                '}';
    }
}
