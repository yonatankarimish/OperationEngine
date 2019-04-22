package com.SixSense.data.commands;

import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalCondition;
import com.SixSense.data.retention.VariableRetention;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractCommand implements ICommand{
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour
    protected final UUID uuid; //This is not the database id of the command, but a uuid for use by components like the session engine
    protected boolean alreadyExecuted;

    protected List<ExecutionCondition> executionConditions;
    protected LogicalCondition conditionAggregation;

    protected List<ExpectedOutcome> expectedOutcomes;
    protected LogicalCondition outcomeAggregation;
    protected String aggregatedOutcomeMessage;

    protected Map<String, String> dynamicFields;
    protected VariableRetention saveTo;

    /*Try not to pollute with additional constructors
    * The empty constructor is for using the 'with' design pattern
    * The parameterized constructor is for conditions and results only */
    public AbstractCommand(){
        this.uuid = UUID.randomUUID();
        this.alreadyExecuted = false;

        this.executionConditions = new ArrayList<>();
        this.conditionAggregation = LogicalCondition.OR;

        this.expectedOutcomes = new ArrayList<>();
        this.outcomeAggregation = LogicalCondition.OR;
        this.aggregatedOutcomeMessage = "";

        this.dynamicFields = new HashMap<>();
        this.saveTo = new VariableRetention();
    }

    public AbstractCommand(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage) {
        this.uuid = UUID.randomUUID();
        this.alreadyExecuted = false;

        this.executionConditions = executionConditions;
        this.conditionAggregation = conditionAggregation;

        this.expectedOutcomes = expectedOutcomes;
        this.outcomeAggregation = outcomeAggregation;
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;

        this.dynamicFields = new HashMap<>();
        this.saveTo = new VariableRetention();
    }

    @Override
    public String getUUID() {
        return uuid.toString();
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
    public List<ExecutionCondition> getExecutionConditions() {
        return Collections.unmodifiableList(executionConditions);
    }

    @Override
    public AbstractCommand addExecutionCondition(ExecutionCondition executionCondition) {
        this.executionConditions.add(executionCondition);
        return this;
    }

    @Override
    public AbstractCommand addExecutionConditions(List<ExecutionCondition> executionConditions) {
        this.executionConditions.addAll(executionConditions);
        return this;
    }

    @Override
    public LogicalCondition getConditionAggregation() {
        return conditionAggregation;
    }

    @Override
    public void setConditionAggregation(LogicalCondition conditionAggregation) {
        this.conditionAggregation = conditionAggregation;
    }

    @Override
    public AbstractCommand withConditionAggregation(LogicalCondition conditionAggregation) {
        this.conditionAggregation = conditionAggregation;
        return this;
    }

    @Override
    public List<ExpectedOutcome> getExpectedOutcomes() {
        return Collections.unmodifiableList(expectedOutcomes);
    }

    @Override
    public AbstractCommand addExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes.addAll(expectedOutcomes);
        return this;
    }

    @Override
    public AbstractCommand addExpectedOutcome(ExpectedOutcome expectedOutcome) {
        this.expectedOutcomes.add(expectedOutcome);
        return this;
    }

    @Override
    public LogicalCondition getOutcomeAggregation() {
        return outcomeAggregation;
    }

    @Override
    public void setOutcomeAggregation(LogicalCondition outcomeAggregation) {
        this.outcomeAggregation = outcomeAggregation;
    }

    @Override
    public AbstractCommand withOutcomeAggregation(LogicalCondition outcomeAggregation) {
        this.outcomeAggregation = outcomeAggregation;
        return this;
    }

    @Override
    public String getAggregatedOutcomeMessage() {
        return aggregatedOutcomeMessage;
    }

    @Override
    public void setAggregatedOutcomeMessage(String aggregatedOutcomeMessage) {
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;
    }

    @Override
    public AbstractCommand withAggregatedOutcomeMessage(String aggregatedOutcomeMessage) {
        this.aggregatedOutcomeMessage = aggregatedOutcomeMessage;
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
    public VariableRetention getSaveTo() {
        return saveTo;
    }

    @Override
    public void setSaveTo(VariableRetention saveTo) {
        this.saveTo = saveTo;
    }

    @Override
    public AbstractCommand withSaveTo(VariableRetention saveTo) {
        this.saveTo = saveTo;
        return this;
    }

    protected AbstractCommand withSuperCloneState(AbstractCommand creator){
        List<ExecutionCondition> conditionClone = creator.executionConditions.stream().map(ExecutionCondition::deepClone).collect(Collectors.toList());
        List<ExpectedOutcome> outcomeClone = creator.expectedOutcomes.stream().map(ExpectedOutcome::deepClone).collect(Collectors.toList());
        this.executionConditions.clear();
        this.expectedOutcomes.clear();

        return this.withAlreadyExecuted(false)
                .addExecutionConditions(conditionClone)
                .withConditionAggregation(creator.conditionAggregation)
                .addExpectedOutcomes(outcomeClone)
                .withOutcomeAggregation(creator.outcomeAggregation)
                .withAggregatedOutcomeMessage(creator.aggregatedOutcomeMessage)
                .addDynamicFields(creator.dynamicFields)
                .withSaveTo(creator.saveTo.deepClone());
    }

    protected String superToString(){
        return  " uuid=" + uuid +
                ", alreadyExecuted=" + alreadyExecuted +
                ", executionConditions=" + executionConditions +
                ", conditionAggregation=" + conditionAggregation +
                ", expectedOutcomes=" + expectedOutcomes +
                ", outcomeAggregation=" + outcomeAggregation +
                ", aggregatedOutcomeMessage='" + aggregatedOutcomeMessage + '\'' +
                ", dynamicFields=" + dynamicFields +
                ", saveTo=" + saveTo;
    }
}
