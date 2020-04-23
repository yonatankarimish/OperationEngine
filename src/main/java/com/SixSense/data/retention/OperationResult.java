package com.SixSense.data.retention;

import com.SixSense.data.interfaces.IEquatable;
import com.SixSense.data.logic.ExpressionResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OperationResult implements IEquatable<OperationResult> {
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
    public boolean weakEquals(OperationResult other) {
        return this.expressionResult.equals(other.expressionResult);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        } else {
            return this.equals((OperationResult) other);
        }
    }

    public boolean equals(OperationResult other) {
        return this.weakEquals(other) &&
            this.databaseVariables.keySet().equals(other.databaseVariables.keySet());
    }

    @Override
    public boolean strongEquals(OperationResult other) {
        return this.weakEquals(other) && this.databaseVariables.equals(other.databaseVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expressionResult, databaseVariables.keySet());
    }

    @Override
    public String toString() {
        return "OperationResult{" +
            "expressionResult=" + expressionResult +
            ", databaseVariables=" + databaseVariables +
            '}';
    }
}
