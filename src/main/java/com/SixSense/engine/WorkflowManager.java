package com.SixSense.engine;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.commands.ParallelWorkflow;
import com.SixSense.data.logic.*;
import com.SixSense.queue.WorkerQueue;
import com.SixSense.util.LogicalExpressionResolver;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkflowManager {
    private static final Logger logger = LogManager.getLogger(WorkflowManager.class);
    @Autowired private SessionEngine sessionEngine;
    @Autowired private WorkerQueue workerQueue;
    private final Map<String, ParallelWorkflow> parentWorkflows;

    private WorkflowManager() {
        this.parentWorkflows = Collections.synchronizedMap(new HashMap<>());
    }

    public void attemptToExecute(ParallelWorkflow workflow){
        workflow.incrementCompletedParentWorkflows();
        if(workflow.getTotalParentWorkflows() == workflow.getCompletedParentWorkflows()){
            boolean executionConditionsMet = LogicalExpressionResolver.resolveLogicalExpression(
                    workflow.getDynamicFields(),
                    workflow.getExecutionCondition()
            ).isResolved();

            if(executionConditionsMet) {
                executeWorkflow(workflow);
            }
        }
    }

    private void executeWorkflow(ParallelWorkflow workflow){
        for(Operation operation : workflow.getParallelOperations()){
            this.parentWorkflows.put(operation.getUUID(), workflow);
            executeWorkflow(operation);
        }
    }

    private void executeWorkflow(Operation operation){
        try {
            workerQueue.submit(() -> sessionEngine.executeOperation(operation));
        }catch (Exception e){
            logger.error("Failed to submit operation " + operation.getUUID() + " to worker queue. Caused by: ", e);
        }
    }

    public void notifyWorkflow(Operation operation, ExpressionResult resolvedOutcome){
        //Operations always use SELF_SEQUENCE_EAGER. Therefore, execute their sequential workflows
        if(resolvedOutcome.getOutcome().equals(ResultStatus.SUCCESS)) {
            for (ParallelWorkflow sequence : operation.getSequentialWorkflowUponSuccess()) {
                attemptToExecute(sequence);
            }
        }else{
            for (ParallelWorkflow sequence : operation.getSequentialWorkflowUponFailure()) {
                attemptToExecute(sequence);
            }
        }

        //Operations always use PARENT_NOTIFICATION_EAGER. Therefore, if the operation is part of a parallel workflow, notify it's parent
        //Note that the container workflow will not be null only if the resolved operation was executed as part of a parallel workflow
        ParallelWorkflow container = this.parentWorkflows.get(operation.getUUID());
        if (container != null) {
            notifyWorkflow(container, resolvedOutcome);
        }
    }

    public void notifyWorkflow(ParallelWorkflow workflow, ExpressionResult resolvedOutcome){
        boolean successful = resolvedOutcome.getOutcome().equals(ResultStatus.SUCCESS);
        boolean allChildrenCompleted = workflow.getCompletedOperations() == workflow.getTotalOperations();
        Set<WorkflowPolicy> workflowPolicies = workflow.getWorkflowPolicies();

        workflow.addOperationOutcomes(resolvedOutcome);

        if(successful) {
            if(workflowPolicies.contains(WorkflowPolicy.SELF_SEQUENCE_EAGER) || allChildrenCompleted) {
                for (ParallelWorkflow sequence : workflow.getSequentialWorkflowUponSuccess()) {
                    attemptToExecute(sequence);
                }
            }
        }else{
            if(workflowPolicies.contains(WorkflowPolicy.SELF_SEQUENCE_EAGER) || allChildrenCompleted) {
                for (ParallelWorkflow sequence : workflow.getSequentialWorkflowUponFailure()) {
                    attemptToExecute(sequence);
                }
            }

            if(workflowPolicies.contains(WorkflowPolicy.OPERATIONS_DEPENDENT)){
                try {
                    for (Operation operation : workflow.getParallelOperations()) {
                        if (!operation.isAlreadyExecuted()) {
                            workerQueue.submit(() -> sessionEngine.terminateOperation(operation.getUUID()));
                        }
                    }
                }catch (Exception e){
                    logger.error("Failed to remove running operations from worker queue. Caused by: ", e);
                }
            }
        }
    }
}
