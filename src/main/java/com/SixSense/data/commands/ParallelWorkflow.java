package com.SixSense.data.commands;

import com.SixSense.data.interfaces.IDeepCloneable;
import com.SixSense.data.interfaces.IEquatable;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.OperationResult;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParallelWorkflow extends AbstractWorkflow implements ICommand, IWorkflow, IDeepCloneable<ParallelWorkflow>, IEquatable<ParallelWorkflow> {
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour
    private List<Operation> parallelOperations;
    private Map<String, OperationResult> operationOutcomes; //will gradually fill with the resolved outcomes of parallel operations. //key: operation id, value: operation result
    private Set<WorkflowPolicy> workflowPolicies;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for conditions, policies and sequential workflows*/
    public ParallelWorkflow() {
        super();
        this.parallelOperations = new ArrayList<>();
        this.operationOutcomes = new HashMap<>();
        this.workflowPolicies = EnumSet.of(
            WorkflowPolicy.OPERATIONS_INDEPENDENT,
            WorkflowPolicy.SELF_SEQUENCE_LAZY/*,
            WorkflowPolicy.PARENT_NOTIFICATION_LAZY,
            WorkflowPolicy.OPERATION_SEQUENCE_AGNOSTIC*/
        );
    }

    public ParallelWorkflow(LogicalExpression<ExecutionCondition> executionCondition, LogicalExpression<ExpectedOutcome> expectedOutcome, List<Operation> parallelOperations, List<ParallelWorkflow> sequentialWorkflowUponSuccess, List<ParallelWorkflow> sequentialWorkflowUponFailure, Set<WorkflowPolicy> workflowPolicies) {
        super(executionCondition, expectedOutcome, sequentialWorkflowUponSuccess, sequentialWorkflowUponFailure);
        this.parallelOperations = parallelOperations;
        this.operationOutcomes = new HashMap<>();
        this.workflowPolicies = workflowPolicies;
    }

    public synchronized int getTotalOperations() {
        return parallelOperations.size();
    }

    public synchronized int getCompletedOperations() {
        return operationOutcomes.size();
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

    public synchronized Map<String, OperationResult> getOperationOutcomes() {
        return Collections.unmodifiableMap(operationOutcomes);
    }

    public synchronized ParallelWorkflow addOperationOutcome(String operationId, OperationResult operationOutcome) {
        this.operationOutcomes.put(operationId, operationOutcome);
        return this;
    }

    public synchronized ParallelWorkflow addOperationOutcomes(Map<String, OperationResult> operationOutcomes) {
        this.operationOutcomes.putAll(operationOutcomes);
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
        if(this == workflow) {
            this.parallelOperations.clear();
        }

        return (ParallelWorkflow)workflow
                .addParallelOperations(clonedOperations)
                .addWorkflowPolicies(this.workflowPolicies)
                .withSuperCloneState(this);
    }

    @Override
    public boolean weakEquals(ParallelWorkflow other) {
        return super.weakEquals(other) && this.equals(other);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || this.getClass() != other.getClass()) {
            return false;
        } else {
            ParallelWorkflow otherAsOperation = (ParallelWorkflow) other;
            return super.equals(otherAsOperation) && this.equals(otherAsOperation);
        }
    }

    public boolean equals(ParallelWorkflow other) {
        return this.parallelOperations.equals(other.parallelOperations) &&
            this.operationOutcomes.equals(other.operationOutcomes) &&
            this.workflowPolicies.equals(other.workflowPolicies);
    }

    @Override
    public boolean strongEquals(ParallelWorkflow other) {
        return super.strongEquals(other) && this.equals(other);
    }

    @Override
    public int hashCode() {
        Stream<Object> childStream = Arrays.stream(new Object[]{parallelOperations, operationOutcomes, workflowPolicies});
        Stream<Object> superStream = Arrays.stream(superMembers());

        Object[] mergedMembers = Stream.concat(superStream, childStream).toArray();
        return Arrays.hashCode(mergedMembers);
    }

    @Override
    public String toString() {
        return "ParallelWorkflow{" +
            "parallelOperations=" + parallelOperations +
            ", operationOutcomes=" + operationOutcomes +
            ", workflowPolicies=" + workflowPolicies +
            ", " + super.superToString() +
            '}';
    }
}
