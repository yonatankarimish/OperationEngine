package com.SixSense.data.logic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@class")
@JsonSubTypes({
    @JsonSubTypes.Type(value=LogicalExpression.class, name = "LogicalExpression"),
    @JsonSubTypes.Type(value=ExecutionCondition.class, name = "ExecutionCondition"),
    @JsonSubTypes.Type(value=ExpectedOutcome.class, name = "ExpectedOutcome")
})
public interface IResolvable {
    ExpressionResult getExpressionResult();

    void setExpressionResult(ExpressionResult expressionResult);

    IResolvable withExpressionResult(ExpressionResult expressionResult);

    IResolvable deepClone(); //Returns a new instance of the same resolvable in its pristine state. That is - as if the new state was never executed

    String toPrettyString();
}
