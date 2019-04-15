package com.SixSense.data.commands;

import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractWorkflow extends AbstractCommand implements ICommand, IWorkflow {
    private List<ParallelWorkflow> sequentialWorkflowUponSuccess;
    private List<ParallelWorkflow> sequentialWorkflowUponFailure;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern*/
    public AbstractWorkflow() {
        super();
        this.sequentialWorkflowUponSuccess = new ArrayList<>();
        this.sequentialWorkflowUponFailure = new ArrayList<>();
    }

    //This constructor is for parallel workflows ("Proper" workflows)
    public AbstractWorkflow(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ParallelWorkflow> sequentialWorkflowUponSuccess, List<ParallelWorkflow> sequentialWorkflowUponFailure) {
        super();
        super.executionConditions = executionConditions;
        super.conditionAggregation = conditionAggregation;
        this.sequentialWorkflowUponSuccess = sequentialWorkflowUponSuccess;
        this.sequentialWorkflowUponFailure = sequentialWorkflowUponFailure;
    }

    //This constructor is for Operations, which extend the abstract Workflow class
    public AbstractWorkflow(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage, List<ParallelWorkflow> sequentialWorkflowUponSuccess, List<ParallelWorkflow> sequentialWorkflowUponFailure) {
        super(executionConditions, conditionAggregation, expectedOutcomes, outcomeAggregation, aggregatedOutcomeMessage);
        this.sequentialWorkflowUponSuccess = sequentialWorkflowUponSuccess;
        this.sequentialWorkflowUponFailure = sequentialWorkflowUponFailure;
    }

    @Override
    public List<ParallelWorkflow> getSequentialWorkflowUponSuccess() {
        return Collections.unmodifiableList(sequentialWorkflowUponSuccess);
    }

    @Override
    public AbstractWorkflow addSequentialWorkflowUponSuccess(ParallelWorkflow sequentialWorkflow) {
        this.sequentialWorkflowUponSuccess.add(sequentialWorkflow);
        return this;
    }

    @Override
    public AbstractWorkflow addSequentialWorkflowsUponSuccess(List<ParallelWorkflow> sequentialWorkflow) {
        this.sequentialWorkflowUponSuccess.addAll(sequentialWorkflow);
        return this;
    }

    @Override
    public List<ParallelWorkflow> getSequentialWorkflowUponFailure() {
        return Collections.unmodifiableList(sequentialWorkflowUponFailure);
    }

    @Override
    public AbstractWorkflow addSequentialWorkflowUponFailure(ParallelWorkflow sequentialWorkflow) {
        this.sequentialWorkflowUponFailure.add(sequentialWorkflow);
        return this;
    }

    @Override
    public AbstractWorkflow addSequentialWorkflowsUponFailure(List<ParallelWorkflow> sequentialWorkflow) {
        this.sequentialWorkflowUponFailure.addAll(sequentialWorkflow);
        return this;
    }
}
