package com.sixsense.model.commands;

import java.util.List;

public interface IWorkflow {

    List<ParallelWorkflow> getSequenceUponSuccess();

    AbstractWorkflow addSequenceUponSuccess(ParallelWorkflow sequentialWorkflow);

    AbstractWorkflow addSequencesUponSuccess(List<ParallelWorkflow> sequentialWorkflow);

    List<ParallelWorkflow> getSequenceUponFailure();

    AbstractWorkflow addSequenceUponFailure(ParallelWorkflow sequentialWorkflow);

    AbstractWorkflow addSequencesUponFailure(List<ParallelWorkflow> sequentialWorkflow);

    boolean isSequenceExecutionStarted();

    void setSequenceExecutionStarted(boolean sequenceExecutionStarted);

    AbstractWorkflow withSequenceExecutionStarted(boolean sequenceExecutionStarted);
}
