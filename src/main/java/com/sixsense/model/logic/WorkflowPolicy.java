package com.sixsense.model.logic;

public enum WorkflowPolicy {
    OPERATIONS_INDEPENDENT, //Parallel workflows execute independently of each other
    OPERATIONS_DEPENDENT, //If a parallel workflow fails, terminate the rest of the parallel workflows (fail-fast)

    SELF_SEQUENCE_EAGER, //If an expected outcome has been achieved, run the sequential workflows immediately
    SELF_SEQUENCE_LAZY, //Wait until all parallel workflows have finished before running any sequential workflows

    /* //(?) if a workflow spawns multiple sequential workflows, do actions in one child affect the others? current working assumption is NO;
    PARENT_NOTIFICATION_EAGER, //If one of the parallel workflows fails, immediately notify the parent workflow
    PARENT_NOTIFICATION_LAZY, //Wait until all parallel workflows have finished before notifying the parent workflow*/

    /*OPERATION_SEQUENCE_EAGER, //Override parallel operation SELF_SEQUENCE policy with SELF_SEQUENCE_EAGER
    OPERATION_SEQUENCE_AGNOSTIC, //Each parallel operation manages it's own SELF_SEQUENCE policy
    OPERATION_SEQUENCE_LAZY //Override parallel operation SELF_SEQUENCE policy with SELF_SEQUENCE_LAZY*/
}
