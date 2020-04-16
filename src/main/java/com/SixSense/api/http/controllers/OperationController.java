package com.SixSense.api.http.controllers;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.commands.ParallelWorkflow;
import com.SixSense.data.devices.Credentials;
import com.SixSense.data.devices.RawExecutionConfig;
import com.SixSense.data.retention.OperationResult;
import com.SixSense.engine.SessionEngine;
import com.SixSense.engine.WorkflowManager;
import com.SixSense.mocks.TestingMocks;
import com.SixSense.threading.ThreadingManager;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.PolymorphicJsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/operations")
public class OperationController extends DebuggableHttpController {
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
        return TestingMocks.f5BigIpBackup(
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
            return wrapForHtml(PolymorphicJsonMapper.serialize(TestingMocks.f5BigIpBackup(
                Collections.singletonList(
                    new Credentials()
                        .withHost("172.31.252.179")
                        .withUsername("root")
                        .withPassword("qwe123")
                )
            )));
        } catch (JsonProcessingException e) {
            logger.error(e);
            return "Failed to serialize f5 json, check the logs for a full stack trace";
        }
    }

    @PostMapping("/execute")
    public RawExecutionConfig execute(@RequestBody RawExecutionConfig rawExecutionConfig){
        ParallelWorkflow workflow = CommandUtils.composeWorkflow(rawExecutionConfig);

        Map<String, OperationResult> workflowResult = workflowManager.executeWorkflow(workflow).join();  //key: operation id, value: operation result
        for(Operation operation : workflow.getParallelOperations()) {
            OperationResult result = workflowResult.get(operation.getUUID());
            rawExecutionConfig.addResult(operation.getDynamicFields().get("device.internal.id"), result);
            logger.info("Operation(s) " + operation.getOperationName() + " Completed with result " + result.getExpressionResult().getOutcome());
            logger.info("Result Message: " + result.getExpressionResult().getMessage());
        }

        return rawExecutionConfig;
    }

    @PostMapping("/terminate")
    public Map<String, OperationResult> terminate(@RequestBody Set<String> operationIdSet) {
        Map<String, CompletableFuture<OperationResult>> terminationResults = new HashMap<>();
        for(String operationId : operationIdSet) {
            CompletableFuture<OperationResult> terminationResult = threadingManager.submit(() -> sessionEngine.terminateOperation(operationId));
            terminationResults.put(operationId, terminationResult);
        }

        CompletableFuture<Map<String, OperationResult>> aggregatedTerminations = CompletableFuture.allOf(
            //Wait for all terminations to finish asynchronously
            terminationResults.values().toArray(CompletableFuture[]::new)
        ).thenApply(voidStub -> {
            //and map each operation id to the termination result
            Map<String, OperationResult> resultMap = new HashMap<>();
            for(String operationId : terminationResults.keySet()){
                resultMap.put(operationId, terminationResults.get(operationId).join());
            }
            return resultMap;
        });

        //finally, join the aggregated future with the invoking thread (this one) and return the termination results
        return aggregatedTerminations.join();
    }

    private static String wrapForHtml(String text){
        return "<p style=\"white-space: pre;\">" + text + "</p>";
    }
}
