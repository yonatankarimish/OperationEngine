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
    private List<ParallelWorkflow> sequenceUponSuccess;
    private List<ParallelWorkflow> sequenceUponFailure;
    private volatile boolean sequenceExecutionStarted;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern*/
    public AbstractWorkflow() {
        super();
        this.sequenceUponSuccess = new ArrayList<>();
        this.sequenceUponFailure = new ArrayList<>();
        this.sequenceExecutionStarted = false;
    }

    public AbstractWorkflow(LogicalExpression<ExecutionCondition> executionCondition, LogicalExpression<ExpectedOutcome> expectedOutcome, List<ParallelWorkflow> sequenceUponSuccess, List<ParallelWorkflow> sequenceUponFailure) {
        super(executionCondition, expectedOutcome);
        this.sequenceUponSuccess = sequenceUponSuccess;
        this.sequenceUponFailure = sequenceUponFailure;
        this.sequenceExecutionStarted = false;
    }

    @Override
    public synchronized List<ParallelWorkflow> getSequenceUponSuccess() {
        return Collections.unmodifiableList(sequenceUponSuccess);
    }

    @Override
    public synchronized AbstractWorkflow addSequenceUponSuccess(ParallelWorkflow sequentialWorkflow) {
        this.sequenceUponSuccess.add(sequentialWorkflow);
        return this;
    }

    @Override
    public synchronized AbstractWorkflow addSequencesUponSuccess(List<ParallelWorkflow> sequentialWorkflow) {
        this.sequenceUponSuccess.addAll(sequentialWorkflow);
        return this;
    }

    @Override
    public synchronized List<ParallelWorkflow> getSequenceUponFailure() {
        return Collections.unmodifiableList(sequenceUponFailure);
    }

    @Override
    public synchronized AbstractWorkflow addSequenceUponFailure(ParallelWorkflow sequentialWorkflow) {
        this.sequenceUponFailure.add(sequentialWorkflow);
        return this;
    }

    @Override
    public synchronized AbstractWorkflow addSequencesUponFailure(List<ParallelWorkflow> sequentialWorkflow) {
        this.sequenceUponFailure.addAll(sequentialWorkflow);
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
        List<ParallelWorkflow> successClone = creator.sequenceUponSuccess.stream().map(ParallelWorkflow::deepClone).collect(Collectors.toList());
        List<ParallelWorkflow> failClone = creator.sequenceUponFailure.stream().map(ParallelWorkflow::deepClone).collect(Collectors.toList());

        if(this == creator) {
            this.sequenceUponSuccess.clear();
            this.sequenceUponFailure.clear();
        }

        return ((AbstractWorkflow)super.withSuperCloneState(creator))
                .addSequencesUponSuccess(successClone)
                .addSequencesUponFailure(failClone)
                .withSequenceExecutionStarted(false);
    }

    protected boolean weakEquals(AbstractWorkflow other){
        return super.weakEquals(other) &&
            this.sequenceUponSuccess.equals(other.sequenceUponSuccess) &&
            this.sequenceUponFailure.equals(other.sequenceUponFailure);
    }

    protected boolean equals(AbstractWorkflow other){
        return super.equals(other) &&
            this.sequenceUponSuccess.equals(other.sequenceUponSuccess) &&
            this.sequenceUponFailure.equals(other.sequenceUponFailure);
    }

    protected boolean strongEquals(AbstractWorkflow other){
        return super.strongEquals(other) &&
            this.sequenceUponSuccess.equals(other.sequenceUponSuccess) &&
            this.sequenceUponFailure.equals(other.sequenceUponFailure) &&
            this.sequenceExecutionStarted == other.sequenceExecutionStarted;
    }

    protected Object[] superMembers(){
        Stream<Object> childStream = Arrays.stream(new Object[]{sequenceUponSuccess, sequenceUponFailure});
        Stream<Object> superStream = Arrays.stream(super.superMembers());

        return Stream.concat(superStream, childStream).toArray();
    }

    public String superToString() {
        return " sequentialWorkflowUponSuccess=" + sequenceUponSuccess +
                ", sequentialWorkflowUponFailure=" + sequenceUponFailure +
                ", sequenceExecutionStarted=" + sequenceExecutionStarted +
                ", " + super.superToString();
    }
}
