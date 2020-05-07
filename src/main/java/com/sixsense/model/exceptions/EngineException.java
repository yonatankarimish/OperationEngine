package com.sixsense.model.exceptions;

public class EngineException extends Exception {
    private EngineExceptionType exceptionType;

    public EngineException(EngineExceptionType exceptionType) {
        super();
        this.exceptionType = exceptionType;
    }


    public EngineException(EngineExceptionType exceptionType, String message) {
        super(message);
        this.exceptionType = exceptionType;
    }


    public EngineException(EngineExceptionType exceptionType, String message, Throwable cause) {
        super(message, cause);
        this.exceptionType = exceptionType;
    }


    public EngineException(EngineExceptionType exceptionType, Throwable cause) {
        super(cause);
        this.exceptionType = exceptionType;
    }


    protected EngineException(EngineExceptionType exceptionType, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.exceptionType = exceptionType;
    }

    public EngineExceptionType getExceptionType() {
        return exceptionType;
    }

    public String getExceptionName() {
        return exceptionType.getPrefixAndCode();
    }
}
