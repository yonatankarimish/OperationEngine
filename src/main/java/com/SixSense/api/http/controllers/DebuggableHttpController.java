package com.SixSense.api.http.controllers;

import com.SixSense.data.aspects.MethodInvocation;
import com.SixSense.data.logging.IDebuggable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public abstract class DebuggableHttpController implements IDebuggable {
    private boolean isUnderDebug = false;
    private Set<MethodInvocation> methodInvocations;

    protected DebuggableHttpController(){
        methodInvocations = Collections.synchronizedSet(new LinkedHashSet<>());
    }

    public Set<MethodInvocation> getMethodInvocations() {
        return Collections.unmodifiableSet(methodInvocations);
    }

    public boolean updateMethodInvocations(MethodInvocation methodInvocation){
        if(isUnderDebug){
            methodInvocations.add(methodInvocation);
            return true;
        }else{
            return false;
        }
    }

    public void clearMethodInvocations() {
        this.methodInvocations.clear();
    }

    @Override
    public boolean isUnderDebug() {
        return isUnderDebug;
    }

    @Override
    public void activateDebugMode() {
        this.isUnderDebug = true;
    }
}
