package com.SixSense.data.events;

//Nested according to command hierarchy
public enum EngineEventType {
    SessionCreated,
    OperationStart,
    BlockStart,
    CommandStart,
    InputSent,
    OutputReceived,
    CommandEndEvent,
    BlockEnd,
    OperationEnd,
    SessionClosed,
    ExecutionAnomaly
}
