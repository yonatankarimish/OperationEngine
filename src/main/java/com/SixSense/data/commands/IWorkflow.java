package com.SixSense.data.commands;

import com.SixSense.data.logic.WorkflowPolicy;

import java.util.List;
import java.util.Set;

public interface IWorkflow {
    Set<WorkflowPolicy> getWorkflowPolicies();

    AbstractWorkflow addWorkflowPolicy(WorkflowPolicy workflowPolicy);

    AbstractWorkflow addWorkflowPolicies(Set<WorkflowPolicy> workflowPolicies);

    List<IWorkflow> getSequentialWorkflow();

    AbstractWorkflow addSequentialWorkflow(IWorkflow sequentialWorkflow);

    AbstractWorkflow addSequentialWorkflows(List<IWorkflow> sequentialWorkflow);
}
