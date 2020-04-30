package com.sixsense.model.logic;

import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.interfaces.IEquatable;
import com.sixsense.utillity.ExpressionUtils;

import java.util.Objects;

public class ExpectedOutcome implements IFlowConnector, IDeepCloneable<ExpectedOutcome>, IEquatable<ExpectedOutcome> {
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
    public boolean weakEquals(ExpectedOutcome other) {
        return binaryRelation == other.binaryRelation &&
            this.expectedValue.equals(other.expectedValue);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        } else {
            return this.weakEquals((ExpectedOutcome) other);
        }
    }

    @Override
    public boolean strongEquals(ExpectedOutcome other) {
        return this.weakEquals(other) && this.expressionResult.equals(other.expressionResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(binaryRelation, expectedValue);
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
