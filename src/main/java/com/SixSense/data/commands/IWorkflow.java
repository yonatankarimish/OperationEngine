package com.SixSense.data.commands;

import java.util.List;

public interface IWorkflow {

    List<ParallelWorkflow> getSequentialWorkflowUponSuccess();

    AbstractWorkflow addSequentialWorkflowUponSuccess(ParallelWorkflow sequentialWorkflow);

    AbstractWorkflow addSequentialWorkflowsUponSuccess(List<ParallelWorkflow> sequentialWorkflow);

    List<ParallelWorkflow> getSequentialWorkflowUponFailure();

    AbstractWorkflow addSequentialWorkflowUponFailure(ParallelWorkflow sequentialWorkflow);

    AbstractWorkflow addSequentialWorkflowsUponFailure(List<ParallelWorkflow> sequentialWorkflow);
}
