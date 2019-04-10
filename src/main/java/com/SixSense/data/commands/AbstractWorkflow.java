package com.SixSense.data.commands;

import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalCondition;
import com.SixSense.data.logic.WorkflowPolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractWorkflow extends AbstractCommand implements ICommand, IWorkflow {
    private Set<WorkflowPolicy> workflowPolicies;
    private List<IWorkflow> sequentialWorkflow;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern*/
    public AbstractWorkflow(){
        super();
        this.workflowPolicies = Stream.of(
                WorkflowPolicy.Independent,
                WorkflowPolicy.LazyNotification,
                WorkflowPolicy.LazySequence
        ).collect(Collectors.toCollection(HashSet::new));
        this.sequentialWorkflow = new ArrayList<>();
    }

    //This constructor is for parallel workflows ("Proper" workflows)
    public AbstractWorkflow(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, Set<WorkflowPolicy> workflowPolicies, List<IWorkflow> sequentialWorkflow) {
        super();
        super.executionConditions = executionConditions;
        super.conditionAggregation = conditionAggregation;
        this.workflowPolicies = workflowPolicies;
        this.sequentialWorkflow = sequentialWorkflow;
    }

    //This constructor is for Operations, which extend the abstract Workflow class
    public AbstractWorkflow(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage, Set<WorkflowPolicy> workflowPolicies, List<IWorkflow> sequentialWorkflow) {
        super(executionConditions, conditionAggregation, expectedOutcomes, outcomeAggregation, aggregatedOutcomeMessage);
        this.workflowPolicies = workflowPolicies;
        this.sequentialWorkflow = sequentialWorkflow;
    }

    @Override
    public Set<WorkflowPolicy> getWorkflowPolicies() {
        return workflowPolicies;
    }

    @Override
    public AbstractWorkflow addWorkflowPolicy(WorkflowPolicy workflowPolicy) {
        this.workflowPolicies.add(workflowPolicy);
        return this;
    }

    @Override
    public AbstractWorkflow addWorkflowPolicies(Set<WorkflowPolicy> workflowPolicies) {
        this.workflowPolicies.addAll(workflowPolicies);
        return this;
    }

    @Override
    public List<IWorkflow> getSequentialWorkflow() {
        return sequentialWorkflow;
    }

    @Override
    public AbstractWorkflow addSequentialWorkflow(IWorkflow sequentialWorkflow) {
        this.sequentialWorkflow.add(sequentialWorkflow);
        return this;
    }

    @Override
    public AbstractWorkflow addSequentialWorkflows(List<IWorkflow> sequentialWorkflow) {
        this.sequentialWorkflow.addAll(sequentialWorkflow);
        return this;
    }
}
