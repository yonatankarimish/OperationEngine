package com.sixsense.model.logic;

import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.interfaces.IEquatable;
import com.sixsense.utillity.ExpressionUtils;

import java.util.Objects;

public class ExecutionCondition implements IFlowConnector, IDeepCloneable<ExecutionCondition>, IEquatable<ExecutionCondition> {
    private String variable;
    private BinaryRelation binaryRelation;
    private String expectedValue;
    private ExpressionResult expressionResult;

    //The default Execution condition wills search for the empty string in any session field, therefore always returning CommandResult.SUCCESS.
    public ExecutionCondition(){
        this.variable = "";
        this.binaryRelation = BinaryRelation.CONTAINS;
        this.expectedValue = "";
        this.expressionResult = new ExpressionResult();
    }

    public ExecutionCondition(String variable, BinaryRelation binaryRelation, String expectedValue) {
        this.variable = variable;
        this.binaryRelation = binaryRelation;
        this.expectedValue = expectedValue;
        this.expressionResult = new ExpressionResult();
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
    public ExpressionResult getExpressionResult() {
        return expressionResult;
    }

    @Override
    public void setExpressionResult(ExpressionResult expressionResult) {
        this.expressionResult = expressionResult;
    }

    @Override
    public ExecutionCondition withExpressionResult(ExpressionResult expressionResult) {
        this.expressionResult = expressionResult;
        return this;
    }

    public LogicalExpression<ExecutionCondition> mergeResolvable(ExecutionCondition additional){
        return ExpressionUtils.mergeExpressions(this, additional);
    }

    public LogicalExpression<ExecutionCondition> mergeExpression(LogicalExpression<ExecutionCondition> additional){
        return ExpressionUtils.mergeExpressions(this, additional);
    }

    //Returns a new instance of the same execution condition in its pristine state. That is - as if the new state was never resolved
    @Override
    public ExecutionCondition deepClone(){
        return new ExecutionCondition()
                .withVariable(this.variable)
                .withBinaryRelation(this.binaryRelation)
                .withExpectedValue(this.expectedValue)
                .withExpressionResult(this.expressionResult.deepClone());
    }

    @Override
    public boolean weakEquals(ExecutionCondition other) {
        return this.variable.equals(other.variable) &&
            this.binaryRelation == other.binaryRelation &&
            this.expectedValue.equals(other.expectedValue);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        } else {
            return this.weakEquals((ExecutionCondition)other);
        }
    }

    @Override
    public boolean strongEquals(ExecutionCondition other) {
        return this.weakEquals(other) && this.expressionResult.equals(other.expressionResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variable, binaryRelation, expectedValue);
    }

    @Override
    public String toPrettyString(){
        String expectedValOrEmpty = expectedValue.isBlank()? "{Empty}" : expectedValue;
        return variable + " " + binaryRelation.name().toLowerCase() + " " + expectedValOrEmpty + " => " + expressionResult.getOutcome().name().toLowerCase();
    }

    @Override
    public String toString() {
        return "ExecutionCondition{" +
                "variable='" + variable + '\'' +
                ", binaryRelation=" + binaryRelation +
                ", expectedValue='" + expectedValue + '\'' +
                ", expressionResult=" + expressionResult +
                '}';
    }
}
