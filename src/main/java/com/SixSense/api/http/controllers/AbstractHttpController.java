package com.SixSense.api.http.controllers;

import com.SixSense.data.logging.IDebuggable;

public abstract class AbstractHttpController implements IDebuggable {
    private boolean isUnderDebug = false;

    @Override
    public boolean isUnderDebug() {
        return isUnderDebug;
    }

    @Override
    public void activateDebugMode() {
        this.isUnderDebug = true;
    }
}
