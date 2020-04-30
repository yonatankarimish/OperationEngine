package com.sixsense.model.events;

//Nested according to command hierarchy
public enum EngineEventType {
    NotInSession,
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
