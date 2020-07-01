package com.sixsense.api.http.controllers;

import com.sixsense.api.ApiDebuggingAware;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.commands.ParallelWorkflow;
import com.sixsense.model.devices.Credentials;
import com.sixsense.model.wrappers.RawExecutionConfig;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.model.wrappers.RawTerminationConfig;
import com.sixsense.services.SessionEngine;
import com.sixsense.services.WorkflowManager;
import com.sixsense.mocks.OperationMocks;
import com.sixsense.threading.ThreadingManager;
import com.sixsense.utillity.CommandUtils;
import com.sixsense.utillity.DynamicFieldGlossary;
import com.sixsense.utillity.PolymorphicJsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/operations")
public class OperationController extends ApiDebuggingAware {
    private static final Logger logger = LogManager.getLogger(OperationController.class);
    private final SessionEngine sessionEngine;
    private final WorkflowManager workflowManager;
    private final ThreadingManager threadingManager;

    @Autowired
    public OperationController(SessionEngine sessionEngine, WorkflowManager workflowManager, ThreadingManager threadingManager) {
        super();
        this.sessionEngine = sessionEngine;
        this.workflowManager = workflowManager;
        this.threadingManager = threadingManager;
    }

    @GetMapping("/f5Config")
    public RawExecutionConfig f5Raw() {
        return OperationMocks.f5BigIpBackup(
            Collections.singletonList(
                new Credentials()
                    .withHost("172.31.252.179")
                    .withUsername("root")
                    .withPassword("qwe123")
            )
        );
    }

    @GetMapping("/f5Json")
    public String f5Json() {
        try {
            return wrapForHtml(PolymorphicJsonMapper.serialize(OperationMocks.f5BigIpBackup(
                Collections.singletonList(
                    new Credentials()
                        .withHost("172.31.252.179")
                        .withUsername("root")
                        .withPassword("qwe123")
                )
            )));
        } catch (JsonProcessingException e) {
            return "Failed to serialize f5 json and wrap for html display. Caused by: " + e.getMessage();
        }
    }

    @PostMapping("/execute")
    public RawExecutionConfig execute(@RequestBody RawExecutionConfig rawExecutionConfig){
        rawExecutionConfig.setStartTime(Instant.now());
        ParallelWorkflow workflow = CommandUtils.composeWorkflow(rawExecutionConfig);

        Map<String, OperationResult> workflowResult = workflowManager.executeWorkflow(workflow).join();  //key: operation id, value: operation result
        for(Operation operation : workflow.getParallelOperations()) {
            OperationResult result = workflowResult.get(operation.getUUID());
            rawExecutionConfig.addResult(operation.getDynamicFields().get(DynamicFieldGlossary.device_internal_id), result);
        }

        rawExecutionConfig.setEndTime(Instant.now());
        return rawExecutionConfig;
    }

    @PostMapping("/terminate")
    public RawTerminationConfig terminate(@RequestBody Set<String> operationIdSet) {
        RawTerminationConfig rawTerminationConfig = new RawTerminationConfig(operationIdSet);
        rawTerminationConfig.setStartTime(Instant.now());

        Map<String, CompletableFuture<OperationResult>> futureTerminations = new HashMap<>();
        for(String operationId : rawTerminationConfig.getOperationIds()) {
            CompletableFuture<OperationResult> futureTermination = threadingManager.submit(() -> sessionEngine.terminateOperation(operationId));
            futureTerminations.put(operationId, futureTermination);
        }

        CompletableFuture.allOf(
            //Wait for all terminations to finish asynchronously
            futureTerminations.values().toArray(CompletableFuture[]::new)
        ).thenAccept(voidStub -> {
            //map each operation id to the termination result
            for(Map.Entry<String, CompletableFuture<OperationResult>> termination : futureTerminations.entrySet()){
                String operationId = termination.getKey();
                rawTerminationConfig.addResult(operationId, termination.getValue().join()); //add to the raw config
            }
        }).join(); //and join the aggregated future with the invoking thread (this one)


        rawTerminationConfig.setEndTime(Instant.now());
        return rawTerminationConfig;
    }

    private static String wrapForHtml(String text){
        return "<p style=\"white-space: pre;\">" + text + "</p>";
    }
}
