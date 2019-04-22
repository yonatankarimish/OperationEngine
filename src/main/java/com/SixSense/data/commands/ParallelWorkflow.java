package com.SixSense.data.commands;

import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalCondition;
import com.SixSense.data.logic.WorkflowPolicy;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParallelWorkflow extends AbstractWorkflow implements ICommand, IWorkflow {
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour
    private volatile int totalParentWorkflows;
    private volatile int completedParentWorkflows;

    private volatile int completedOperations;
    private List<Operation> parallelOperations;
    private List<ExpectedOutcome> operationOutcomes; //will gradually fill with the resolved outcomes of parallel operations
    private Set<WorkflowPolicy> workflowPolicies;

    public ParallelWorkflow() {
        super();
        this.totalParentWorkflows = 0;
        this.completedParentWorkflows = 0;

        this.completedOperations = 0;
        this.parallelOperations = new ArrayList<>();
        this.operationOutcomes = new ArrayList<>();
        this.workflowPolicies = Stream.of(
                WorkflowPolicy.OPERATIONS_INDEPENDENT,
                WorkflowPolicy.SELF_SEQUENCE_LAZY/*,
                WorkflowPolicy.PARENT_NOTIFICATION_LAZY,
                WorkflowPolicy.OPERATION_SEQUENCE_AGNOSTIC*/
        ).collect(Collectors.toCollection(HashSet::new));
    }

    public ParallelWorkflow(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<Operation> parallelOperations, List<ParallelWorkflow> sequentialWorkflowUponSuccess, List<ParallelWorkflow> sequentialWorkflowUponFailure, Set<WorkflowPolicy> workflowPolicies) {
        super(executionConditions, conditionAggregation, sequentialWorkflowUponSuccess, sequentialWorkflowUponFailure);
        this.totalParentWorkflows = 0;
        this.completedParentWorkflows = 0;

        this.completedOperations = 0;
        this.parallelOperations = parallelOperations;
        this.operationOutcomes = new ArrayList<>();
        this.workflowPolicies = workflowPolicies;
    }

    public synchronized int getTotalParentWorkflows() {
        return totalParentWorkflows;
    }

    public synchronized int getCompletedParentWorkflows() {
        return completedParentWorkflows;
    }

    public synchronized ParallelWorkflow incrementCompletedParentWorkflows() {
        completedParentWorkflows++;
        return this;
    }

    public synchronized int getTotalOperations() {
        return parallelOperations.size();
    }

    public synchronized int getCompletedOperations() {
        return completedOperations;
    }

    public synchronized List<Operation> getParallelOperations() {
        return Collections.unmodifiableList(parallelOperations);
    }

    public synchronized ParallelWorkflow addParallelOperation(Operation sequentialWorkflow) {
        this.parallelOperations.add(sequentialWorkflow);
        return this;
    }

    public synchronized ParallelWorkflow addParallelOperations(List<Operation> sequentialWorkflow) {
        this.parallelOperations.addAll(sequentialWorkflow);
        return this;
    }

    public synchronized List<ExpectedOutcome> getOperationOutcomes() {
        return Collections.unmodifiableList(operationOutcomes);
    }

    public synchronized ParallelWorkflow addOperationOutcomes(ExpectedOutcome operationOutcome) {
        this.operationOutcomes.add(operationOutcome);
        this.completedOperations++;
        return this;
    }

    public Set<WorkflowPolicy> getWorkflowPolicies() {
        return Collections.unmodifiableSet(workflowPolicies);
    }

    public ParallelWorkflow addWorkflowPolicy(WorkflowPolicy workflowPolicy) {
        this.workflowPolicies.add(workflowPolicy);
        return this;
    }

    public ParallelWorkflow addWorkflowPolicies(Set<WorkflowPolicy> workflowPolicies) {
        this.workflowPolicies.addAll(workflowPolicies);
        return this;
    }

    @Override
    public ICommand chainCommands(ICommand additional) {
        throw new UnsupportedOperationException("Not yet supported, but it should be...");
    }

    //Returns a new instance of the same workflow in its pristine state. That is - as if the new state was never executed
    @Override
    public ParallelWorkflow deepClone(){
        return assignDefaults(new ParallelWorkflow());
    }

    //Reverts the same workflow instance to it's pristine state.  That is - as if the same command was never executed
    @Override
    public ParallelWorkflow reset(){
        return assignDefaults(this);
    }

    private ParallelWorkflow assignDefaults(ParallelWorkflow workflow){
        List<Operation> clonedOperations = this.parallelOperations.stream().map(Operation::deepClone).collect(Collectors.toList());
        this.parallelOperations.clear();

        return (ParallelWorkflow)workflow
                .addParallelOperations(clonedOperations)
                .addWorkflowPolicies(this.workflowPolicies)
                .withSuperCloneState(this);
    }

    @Override
    public String toString() {
        return "ParallelWorkflow{" +
                "totalParentWorkflows=" + totalParentWorkflows +
                ", completedParentWorkflows=" + completedParentWorkflows +
                ", completedOperations=" + completedOperations +
                ", parallelOperations=" + parallelOperations +
                ", operationOutcomes=" + operationOutcomes +
                ", workflowPolicies=" + workflowPolicies +
                ", " + super.superToString() +
                '}';
    }
}
