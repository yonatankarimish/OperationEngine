package com.SixSense.data.outcomes;

public interface IFlowConnector {
    BinaryRelation getBinaryRelation();

    String getExpectedValue();

    boolean isResolved();

    void setResolved(boolean resolved);

    IFlowConnector withResolved(boolean resolved);
}
