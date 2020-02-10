package com.SixSense.web;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.commands.ParallelWorkflow;
import com.SixSense.data.devices.Credentials;
import com.SixSense.data.devices.RawExecutionConfig;
import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.engine.WorkflowManager;
import com.SixSense.mocks.TestingMocks;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.PolymorphicJsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OperationController {
    private static final Logger logger = LogManager.getLogger(OperationController.class);
    private final WorkflowManager workflowManager;

    @Autowired
    public OperationController(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
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

        Map<String, ExpressionResult> workflowResult = workflowManager.executeWorkflow(workflow).join();  //key: operation id, value: operation result
        for(Operation operation : workflow.getParallelOperations()) {
            ExpressionResult result = workflowResult.get(operation.getUUID());
            rawExecutionConfig.addResult(operation.getDynamicFields().get("device.internal.id"), result);
            logger.info("Operation(s) " + operation.getOperationName() + " Completed with result " + result.getOutcome());
            logger.info("Result Message: " + result.getMessage());
        }

        return rawExecutionConfig;
    }

    private static String wrapForHtml(String text){
        return "<p style=\"white-space: pre;\">" + text + "</p>";
    }
}
