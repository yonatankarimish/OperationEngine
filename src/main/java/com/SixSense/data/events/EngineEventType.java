package com.SixSense.data.events;

//Nested according to command hierarchy
public enum EngineEventType {
    SessionCreated,
    OperationStart,
    BlockStart,
    CommandStart,
    InputSent,
    OutputReceived,
    CommandEnd,
    BlockEnd,
    OperationEnd,
    SessionClosed,
    ConditionEvaluation,
    OutcomeEvaluation,
    LoopingEvaluation,
    ResultRetention,
    ExecutionAnomaly
}
