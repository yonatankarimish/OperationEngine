package com.sixsense.model.logic;

public interface IFlowConnector extends IResolvable{
    BinaryRelation getBinaryRelation();

    String getExpectedValue();

    ExpressionResult getExpressionResult();

    void setExpressionResult(ExpressionResult expressionResult);

    IFlowConnector withExpressionResult(ExpressionResult expressionResult);

    IFlowConnector deepClone(); //Returns a new instance of the same flow connector in its pristine state. That is - as if the new state was never executed
}
