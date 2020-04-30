package com.sixsense.model.commands;

import com.sixsense.model.logic.ExecutionCondition;
import com.sixsense.model.logic.ExpectedOutcome;
import com.sixsense.model.logic.LogicalExpression;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractWorkflow extends AbstractCommand implements ICommand, IWorkflow {
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour

    //Theoretically this could be a single workflow, but then we would need to aggregate the operations
    //and we could not use different outcomes for each sequential workflow
    private List<ParallelWorkflow> sequentialWorkflowUponSuccess;
    private List<ParallelWorkflow> sequentialWorkflowUponFailure;
    private volatile boolean sequenceExecutionStarted;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern*/
    public AbstractWorkflow() {
        super();
        this.sequentialWorkflowUponSuccess = new ArrayList<>();
        this.sequentialWorkflowUponFailure = new ArrayList<>();
        this.sequenceExecutionStarted = false;
    }

    public AbstractWorkflow(LogicalExpression<ExecutionCondition> executionCondition, LogicalExpression<ExpectedOutcome> expectedOutcome, List<ParallelWorkflow> sequentialWorkflowUponSuccess, List<ParallelWorkflow> sequentialWorkflowUponFailure) {
        super(executionCondition, expectedOutcome);
        this.sequentialWorkflowUponSuccess = sequentialWorkflowUponSuccess;
        this.sequentialWorkflowUponFailure = sequentialWorkflowUponFailure;
        this.sequenceExecutionStarted = false;
    }

    @Override
    public synchronized List<ParallelWorkflow> getSequentialWorkflowUponSuccess() {
        return Collections.unmodifiableList(sequentialWorkflowUponSuccess);
    }

    @Override
    public synchronized AbstractWorkflow addSequentialWorkflowUponSuccess(ParallelWorkflow sequentialWorkflow) {
        this.sequentialWorkflowUponSuccess.add(sequentialWorkflow);
        return this;
    }

    @Override
    public synchronized AbstractWorkflow addSequentialWorkflowsUponSuccess(List<ParallelWorkflow> sequentialWorkflow) {
        this.sequentialWorkflowUponSuccess.addAll(sequentialWorkflow);
        return this;
    }

    @Override
    public synchronized List<ParallelWorkflow> getSequentialWorkflowUponFailure() {
        return Collections.unmodifiableList(sequentialWorkflowUponFailure);
    }

    @Override
    public synchronized AbstractWorkflow addSequentialWorkflowUponFailure(ParallelWorkflow sequentialWorkflow) {
        this.sequentialWorkflowUponFailure.add(sequentialWorkflow);
        return this;
    }

    @Override
    public synchronized AbstractWorkflow addSequentialWorkflowsUponFailure(List<ParallelWorkflow> sequentialWorkflow) {
        this.sequentialWorkflowUponFailure.addAll(sequentialWorkflow);
        return this;
    }

    @Override
    public boolean isSequenceExecutionStarted() {
        return sequenceExecutionStarted;
    }

    @Override
    public void setSequenceExecutionStarted(boolean sequenceExecutionStarted) {
        this.sequenceExecutionStarted = sequenceExecutionStarted;
    }

    @Override
    public AbstractWorkflow withSequenceExecutionStarted(boolean sequenceExecutionStarted) {
        this.sequenceExecutionStarted = sequenceExecutionStarted;
        return this;
    }

    protected AbstractWorkflow withSuperCloneState(AbstractWorkflow creator){
        List<ParallelWorkflow> successClone = creator.sequentialWorkflowUponSuccess.stream().map(ParallelWorkflow::deepClone).collect(Collectors.toList());
        List<ParallelWorkflow> failClone = creator.sequentialWorkflowUponFailure.stream().map(ParallelWorkflow::deepClone).collect(Collectors.toList());

        if(this == creator) {
            this.sequentialWorkflowUponSuccess.clear();
            this.sequentialWorkflowUponFailure.clear();
        }

        return ((AbstractWorkflow)super.withSuperCloneState(creator))
                .addSequentialWorkflowsUponSuccess(successClone)
                .addSequentialWorkflowsUponFailure(failClone)
                .withSequenceExecutionStarted(false);
    }

    protected boolean weakEquals(AbstractWorkflow other){
        return super.weakEquals(other) &&
            this.sequentialWorkflowUponSuccess.equals(other.sequentialWorkflowUponSuccess) &&
            this.sequentialWorkflowUponFailure.equals(other.sequentialWorkflowUponFailure);
    }

    protected boolean equals(AbstractWorkflow other){
        return super.equals(other) &&
            this.sequentialWorkflowUponSuccess.equals(other.sequentialWorkflowUponSuccess) &&
            this.sequentialWorkflowUponFailure.equals(other.sequentialWorkflowUponFailure);
    }

    protected boolean strongEquals(AbstractWorkflow other){
        return super.strongEquals(other) &&
            this.sequentialWorkflowUponSuccess.equals(other.sequentialWorkflowUponSuccess) &&
            this.sequentialWorkflowUponFailure.equals(other.sequentialWorkflowUponFailure) &&
            this.sequenceExecutionStarted == other.sequenceExecutionStarted;
    }

    protected Object[] superMembers(){
        Stream<Object> childStream = Arrays.stream(new Object[]{sequentialWorkflowUponSuccess, sequentialWorkflowUponFailure});
        Stream<Object> superStream = Arrays.stream(super.superMembers());

        return Stream.concat(superStream, childStream).toArray();
    }

    public String superToString() {
        return " sequentialWorkflowUponSuccess=" + sequentialWorkflowUponSuccess +
                ", sequentialWorkflowUponFailure=" + sequentialWorkflowUponFailure +
                ", sequenceExecutionStarted=" + sequenceExecutionStarted +
                ", " + super.superToString();
    }
}
