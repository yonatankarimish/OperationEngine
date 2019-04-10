package com.SixSense.data.commands;

import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.LogicalCondition;
import com.SixSense.data.logic.WorkflowPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ParallelWorkflow extends AbstractWorkflow implements ICommand, IWorkflow {
    private List<Operation> parallelOperations;

    public ParallelWorkflow() {
        super();
        this.parallelOperations = new ArrayList<>();
    }

    public ParallelWorkflow(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, Set<WorkflowPolicy> workflowPolicies, List<IWorkflow> sequentialWorkflow, List<Operation> parallelOperations) {
        super(executionConditions, conditionAggregation, workflowPolicies, sequentialWorkflow);
        this.parallelOperations = parallelOperations;
    }

    public List<Operation> getParallelOperations() {
        return parallelOperations;
    }

    public ParallelWorkflow addParallelOperation(Operation sequentialWorkflow) {
        this.parallelOperations.add(sequentialWorkflow);
        return this;
    }

    public ParallelWorkflow addParallelOperations(List<Operation> sequentialWorkflow) {
        this.parallelOperations.addAll(sequentialWorkflow);
        return this;
    }

    @Override
    public ICommand chainCommands(ICommand additional) {
        throw new UnsupportedOperationException("Not yet supported, but it should be...");
    }

    @Override
    public String toString() {
        return "ParallelWorkflow{" +
                "parallelOperations=" + parallelOperations +
                ", alreadyExecuted=" + alreadyExecuted +
                ", executionConditions=" + executionConditions +
                ", conditionAggregation=" + conditionAggregation +
                ", expectedOutcomes=" + expectedOutcomes +
                ", outcomeAggregation=" + outcomeAggregation +
                ", aggregatedOutcomeMessage='" + aggregatedOutcomeMessage + '\'' +
                ", dynamicFields=" + dynamicFields +
                ", saveTo=" + saveTo +
                '}';
    }
}
