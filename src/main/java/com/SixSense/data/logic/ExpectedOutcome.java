package com.SixSense.data.logic;

import com.SixSense.data.IDeepCloneable;
import com.SixSense.util.ExpressionUtils;

public class ExpectedOutcome implements IFlowConnector, IDeepCloneable<ExpectedOutcome> {
    private BinaryRelation binaryRelation;
    private String expectedValue;
    private ExpressionResult expressionResult;

    //The default expected outcome wills search for the empty string in any result, therefore always returning CommandResult.SUCCESS.
    public ExpectedOutcome() {
        this.binaryRelation = BinaryRelation.CONTAINS;
        this.expectedValue = "";
        this.expressionResult = new ExpressionResult();
    }

    public ExpectedOutcome(BinaryRelation binaryRelation, String expectedValue) {
        this.binaryRelation = binaryRelation;
        this.expectedValue = expectedValue;
        this.expressionResult = new ExpressionResult();
    }

    @Override
    public BinaryRelation getBinaryRelation() {
        return binaryRelation;
    }

    public void setBinaryRelation(BinaryRelation binaryRelation) {
        this.binaryRelation = binaryRelation;
    }

    public ExpectedOutcome withBinaryRelation(BinaryRelation binaryRelation) {
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

    public ExpectedOutcome withExpectedValue(String expectedValue) {
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
    public ExpectedOutcome withExpressionResult(ExpressionResult expressionResult) {
        this.expressionResult = expressionResult;
        return this;
    }

    public LogicalExpression<ExpectedOutcome> mergeResolvable(ExpectedOutcome additional){
        return ExpressionUtils.mergeExpressions(this, additional);
    }

    public LogicalExpression<ExpectedOutcome> mergeExpression(LogicalExpression<ExpectedOutcome> additional){
        return ExpressionUtils.mergeExpressions(this, additional);
    }

    //Returns a new instance of the same expected outcome in its pristine state. That is - as if the new state was never resolved
    @Override
    public ExpectedOutcome deepClone(){
        return new ExpectedOutcome()
                .withExpectedValue(this.expectedValue)
                .withBinaryRelation(this.binaryRelation)
                .withExpressionResult(this.expressionResult.deepClone());
    }

    @Override
    public String toPrettyString(){
        String expectedValOrEmpty = expectedValue.isBlank()? "{Empty}" : expectedValue;
        return "$sixsense.result.mark " + binaryRelation.name().toLowerCase() + " " + expectedValOrEmpty + " => " + expressionResult.getOutcome().name().toLowerCase();
    }

    @Override
    public String toString() {
        return "ExpectedOutcome{" +
                "expectedValue='" + expectedValue + '\'' +
                ", binaryRelation=" + binaryRelation +
                ", expressionResult=" + expressionResult +
                '}';
    }
}
