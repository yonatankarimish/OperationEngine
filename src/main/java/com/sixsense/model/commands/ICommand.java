package com.sixsense.model.commands;

import com.sixsense.model.logic.ExecutionCondition;
import com.sixsense.model.logic.ExpectedOutcome;
import com.sixsense.model.logic.LogicalExpression;
import com.sixsense.model.retention.ResultRetention;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@class")
@JsonSubTypes({
    @JsonSubTypes.Type(value=Command.class, name = "Command"),
    @JsonSubTypes.Type(value=Block.class, name = "Block"),
    @JsonSubTypes.Type(value=Operation.class, name = "Operation"),
    @JsonSubTypes.Type(value=ParallelWorkflow.class, name = "ParallelWorkflow")
})
public interface ICommand {
    String getUUID();

    boolean isAlreadyExecuted();

    void setAlreadyExecuted(boolean alreadyExecuted);

    ICommand withAlreadyExecuted(boolean alreadyExecuted);

    LogicalExpression<ExecutionCondition> getExecutionCondition();

    void setExecutionCondition(LogicalExpression<ExecutionCondition> executionCondition);

    AbstractCommand withExecutionCondition(LogicalExpression<ExecutionCondition> executionCondition);

    LogicalExpression<ExpectedOutcome> getExpectedOutcome();

    void setExpectedOutcome(LogicalExpression<ExpectedOutcome> expectedOutcome);

    AbstractCommand withExpectedOutcome(LogicalExpression<ExpectedOutcome> expectedOutcome);

    ICommand chainCommands(ICommand additional);

    Map<String, String> getDynamicFields();

    AbstractCommand addDynamicField(String key, String value);

    AbstractCommand addDynamicFields(Map<String, String> dynamicFields);

    ResultRetention getSaveTo();

    void setSaveTo(ResultRetention saveTo);

    ICommand withSaveTo(ResultRetention saveTo);

    ICommand deepClone(); //Returns a new instance of the same command in its pristine state. That is - as if the new state was never executed

    ICommand reset(); //Reverts the same command instance to it's pristine state.  That is - as if the same command was never executed

}
