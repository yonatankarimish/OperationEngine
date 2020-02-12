package com.SixSense.data.retention;

import com.SixSense.data.logic.ExpressionResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OperationResult {
    private ExpressionResult expressionResult;
    private Map<String, String> databaseVariables;

    public OperationResult() {
        this.expressionResult = new ExpressionResult();
        this.databaseVariables = new HashMap<>();
    }

    public OperationResult(ExpressionResult expressionResult, Map<String, String> databaseVariables) {
        this.expressionResult = expressionResult;
        this.databaseVariables = databaseVariables;
    }

    public ExpressionResult getExpressionResult() {
        return expressionResult;
    }

    public void setExpressionResult(ExpressionResult expressionResult) {
        this.expressionResult = expressionResult;
    }

    public OperationResult withExpressionResult(ExpressionResult expressionResult) {
        this.expressionResult = expressionResult;
        return this;
    }

    public Map<String, String> getDatabaseVariables() {
        return Collections.unmodifiableMap(databaseVariables);
    }

    public OperationResult addDatabaseVariable(String key, String value) {
        this.databaseVariables.put(key, value);
        return this;
    }

    public OperationResult addDatabaseVariables(Map<String, String> databaseVariables) {
        this.databaseVariables.putAll(databaseVariables);
        return this;
    }

    @Override
    public String toString() {
        return "OperationResult{" +
            "expressionResult=" + expressionResult +
            ", databaseVariables=" + databaseVariables +
            '}';
    }
}
