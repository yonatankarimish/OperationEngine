package com.SixSense.data.aspects;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MethodInvocation {
    private UUID uuid; //Internal uuid: method invocations are never equal, even if they are invoked with the same arguments.
    private String methodName;
    private Object[] invocationArguments;
    private CompletableFuture<Object> returnValue; //Wrapped with a future because methods are first invoked, then return a value.

    public MethodInvocation(String methodName, Object[] invocationArguments){
        this.uuid = UUID.randomUUID();
        this.methodName = methodName;
        this.invocationArguments = invocationArguments;
        this.returnValue = new CompletableFuture<>();
    }

    public MethodInvocation(String methodName, Object[] invocationArguments, Object returnValue) {
        this.uuid = UUID.randomUUID();
        this.methodName = methodName;
        this.invocationArguments = invocationArguments;
        this.returnValue = CompletableFuture.completedFuture(returnValue);
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getInvocationArguments() {
        return invocationArguments;
    }

    public CompletableFuture<Object> getReturnValue() {
        return returnValue;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }else if (other == null || this.getClass() != other.getClass()) {
            return false;
        }else {
            MethodInvocation otherAsInvocation = (MethodInvocation) other;
            return this.uuid.equals(otherAsInvocation.uuid);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
