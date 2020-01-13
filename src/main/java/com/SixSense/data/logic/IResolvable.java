package com.SixSense.data.logic;

public interface IResolvable {
    ExpressionResult getExpressionResult();

    void setExpressionResult(ExpressionResult expressionResult);

    IResolvable withExpressionResult(ExpressionResult expressionResult);

    IResolvable deepClone(); //Returns a new instance of the same resolvable in its pristine state. That is - as if the new state was never executed
}
