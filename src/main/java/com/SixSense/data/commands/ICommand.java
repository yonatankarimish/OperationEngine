package com.SixSense.data.commands;

import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalExpression;
import com.SixSense.data.retention.VariableRetention;

import java.util.List;
import java.util.Map;

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

    VariableRetention getSaveTo();

    void setSaveTo(VariableRetention saveTo);

    ICommand withSaveTo(VariableRetention saveTo);

    ICommand deepClone(); //Returns a new instance of the same command in its pristine state. That is - as if the new state was never executed

    ICommand reset(); //Reverts the same command instance to it's pristine state.  That is - as if the same command was never executed

}
