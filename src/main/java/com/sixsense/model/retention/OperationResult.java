package com.sixsense.model.retention;

import com.sixsense.model.interfaces.IEquatable;
import com.sixsense.model.logic.ExpressionResult;

import java.util.*;

public class OperationResult implements IEquatable<OperationResult> {
    private ExpressionResult expressionResult;
    private Set<DatabaseVariable> databaseVariables;

    public OperationResult() {
        this.expressionResult = new ExpressionResult();
        this.databaseVariables = new HashSet<>();
    }

    public OperationResult(ExpressionResult expressionResult, Set<DatabaseVariable> databaseVariables) {
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

    public Set<DatabaseVariable> getDatabaseVariables() {
        return Collections.unmodifiableSet(databaseVariables);
    }

    public OperationResult addDatabaseVariable(DatabaseVariable databaseVariable) {
        this.databaseVariables.add(databaseVariable);
        return this;
    }

    public OperationResult addDatabaseVariables(Set<DatabaseVariable> databaseVariables) {
        this.databaseVariables.addAll(databaseVariables);
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
        return this.weakEquals(other) && this.databaseVariables.equals(other.databaseVariables);
    }

    @Override
    public boolean strongEquals(OperationResult other) {
        return this.weakEquals(other) && this.databaseVariables.equals(other.databaseVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expressionResult, databaseVariables);
    }

    @Override
    public String toString() {
        return "OperationResult{" +
            "expressionResult=" + expressionResult +
            ", databaseVariables=" + databaseVariables +
            '}';
    }
}
