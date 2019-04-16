package com.SixSense.data.logic;

public interface IFlowConnector {
    BinaryRelation getBinaryRelation();

    String getExpectedValue();

    boolean isResolved();

    void setResolved(boolean resolved);

    IFlowConnector withResolved(boolean resolved);

    IFlowConnector deepClone(); //Returns a new instance of the same flow connector in its pristine state. That is - as if the new state was never executed
}
