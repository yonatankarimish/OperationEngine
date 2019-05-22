package com.SixSense.data.logic;

import com.SixSense.util.ExpressionUtils;

public class ExecutionCondition implements IFlowConnector {
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
    public String toString() {
        return "ExecutionCondition{" +
                "variable='" + variable + '\'' +
                ", binaryRelation=" + binaryRelation +
                ", expectedValue='" + expectedValue + '\'' +
                ", expressionResult=" + expressionResult +
                '}';
    }
}
