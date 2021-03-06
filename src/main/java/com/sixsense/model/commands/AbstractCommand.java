package com.sixsense.model.commands;

import com.sixsense.model.logic.ExecutionCondition;
import com.sixsense.model.logic.ExpectedOutcome;
import com.sixsense.model.logic.LogicalExpression;
import com.sixsense.model.retention.ResultRetention;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public abstract class AbstractCommand implements ICommand{
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour
    private final UUID uuid; //This is not the database id of the command, but a uuid for use by components like the session engine
    private boolean alreadyExecuted;

    private LogicalExpression<ExecutionCondition> executionCondition;
    private LogicalExpression<ExpectedOutcome> expectedOutcome;

    private Map<String, String> dynamicFields;
    private ResultRetention saveTo;

    /*Try not to pollute with additional constructors
    * The empty constructor is for using the 'with' design pattern
    * The parameterized constructor is for conditions and results only */
    public AbstractCommand(){
        this.uuid = UUID.randomUUID();
        this.alreadyExecuted = false;

        this.executionCondition = new LogicalExpression<>();
        this.expectedOutcome = new LogicalExpression<>();

        this.dynamicFields = new HashMap<>();
        this.saveTo = new ResultRetention();
    }

    public AbstractCommand(LogicalExpression<ExecutionCondition> executionCondition, LogicalExpression<ExpectedOutcome> expectedOutcome) {
        this.uuid = UUID.randomUUID();
        this.alreadyExecuted = false;

        this.executionCondition = executionCondition;
        this.expectedOutcome = expectedOutcome;

        this.dynamicFields = new HashMap<>();
        this.saveTo = new ResultRetention();
    }

    @Override
    @JsonIgnore
    public String getUUID() {
        return uuid.toString();
    }

    @JsonIgnore
    public String getShortUUID() {
        return uuid.toString().substring(0,8);
    }

    @Override
    public boolean isAlreadyExecuted() {
        return alreadyExecuted;
    }

    @Override
    public void setAlreadyExecuted(boolean alreadyExecuted) {
        this.alreadyExecuted = alreadyExecuted;
    }

    @Override
    public AbstractCommand withAlreadyExecuted(boolean hasBeenExecuted) {
        this.alreadyExecuted = hasBeenExecuted;
        return this;
    }

    @Override
    public LogicalExpression<ExecutionCondition> getExecutionCondition() {
        return executionCondition;
    }

    @Override
    public void setExecutionCondition(LogicalExpression<ExecutionCondition> executionCondition) {
        this.executionCondition = executionCondition;
    }

    @Override
    public AbstractCommand withExecutionCondition(LogicalExpression<ExecutionCondition> executionCondition) {
        this.executionCondition = executionCondition;
        return this;
    }

    @Override
    public LogicalExpression<ExpectedOutcome> getExpectedOutcome() {
        return expectedOutcome;
    }

    @Override
    public void setExpectedOutcome(LogicalExpression<ExpectedOutcome> expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    @Override
    public AbstractCommand withExpectedOutcome(LogicalExpression<ExpectedOutcome> expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
        return this;
    }

    @Override
    public Map<String, String> getDynamicFields() {
        return Collections.unmodifiableMap(this.dynamicFields);
    }

    @Override
    public AbstractCommand addDynamicField(String key, String value) {
        this.dynamicFields.put(key, value);
        return this;
    }

    @Override
    public AbstractCommand addDynamicFields(Map<String, String> dynamicFields) {
        this.dynamicFields.putAll(dynamicFields);
        return this;
    }

    @Override
    public ResultRetention getSaveTo() {
        return saveTo;
    }

    @Override
    public void setSaveTo(ResultRetention saveTo) {
        this.saveTo = saveTo;
    }

    @Override
    public AbstractCommand withSaveTo(ResultRetention saveTo) {
        this.saveTo = saveTo;
        return this;
    }

    protected AbstractCommand withSuperCloneState(AbstractCommand creator){
        if(this == creator) {
            this.dynamicFields.clear();
        }

        return this.withAlreadyExecuted(false)
                .withExecutionCondition(creator.executionCondition.deepClone())
                .withExpectedOutcome(creator.expectedOutcome.deepClone())
                .addDynamicFields(creator.dynamicFields)
                .withSaveTo(creator.saveTo.deepClone());
    }

    protected boolean weakEquals(AbstractCommand other){
        return this.executionCondition.equals(other.executionCondition) &&
            this.expectedOutcome.equals(other.expectedOutcome) &&
            this.saveTo.equals(other.saveTo);
    }

    @Override
    public abstract boolean equals(Object other);

    protected boolean equals(AbstractCommand other){
        return this.weakEquals(other) &&
            this.dynamicFields.keySet().equals(other.dynamicFields.keySet());
    }

    protected boolean strongEquals(AbstractCommand other){
        return this.weakEquals(other) &&
            this.alreadyExecuted == other.alreadyExecuted &&
            this.dynamicFields.equals(other.dynamicFields);
    }

    @Override
    public abstract int hashCode();

    protected Object[] superMembers(){
        return new Object[]{executionCondition, expectedOutcome, saveTo};
    }

    protected String superToString(){
        return  " uuid=" + uuid +
                ", alreadyExecuted=" + alreadyExecuted +
                ", executionCondition=" + executionCondition +
                ", expectedOutcome=" + expectedOutcome +
                ", dynamicFields=" + dynamicFields +
                ", saveTo=" + saveTo;
    }
}
