package com.sixsense.api.amqp;

import com.sixsense.api.ApiDebuggingAware;
import com.sixsense.api.amqp.config.AMQPConfig;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.commands.ParallelWorkflow;
import com.sixsense.model.devices.RawExecutionConfig;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.utillity.PolymorphicJsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OperationProducer extends ApiDebuggingAware {
    private static final Logger logger = LogManager.getLogger(OperationProducer.class);

    //AMQP entities
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange resultExchange;

    @Autowired
    public OperationProducer(RabbitTemplate rabbitTemplate, DirectExchange resultExchange) {
        super();
        this.rabbitTemplate = rabbitTemplate;
        this.resultExchange = resultExchange;
    }

    public void produceOperationResults(RawExecutionConfig rawExecutionConfig, ParallelWorkflow composedWorkflow, Map<String, OperationResult> workflowResult, String queue, long deliveryTag) {
        for (Operation operation : composedWorkflow.getParallelOperations()) {
            OperationResult result = workflowResult.get(operation.getUUID());
            rawExecutionConfig.addResult(operation.getDynamicFields().get("device.internal.id"), result);
            logger.info("Operation(s) " + operation.getOperationName() + " Completed with result " + result.getExpressionResult().getOutcome());
            logger.info("Result Message: " + result.getExpressionResult().getMessage());
        }

        String asJSON = "";
        try {
            asJSON = PolymorphicJsonMapper.serialize(rawExecutionConfig);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize operation result for message with delivery tag " + deliveryTag + " from queue " + queue + ". Caused by: ", e);
            logger.error("Check the engine logs for details about the operation and it's result ");
        }

        if (!asJSON.isEmpty()) {
            Message response = MessageBuilder.withBody(asJSON.getBytes())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbitTemplate.convertAndSend(resultExchange.getName(), AMQPConfig.OperationResultBindingKey, response);
        }
    }

    public void produceRetentionResult(String operationId, ResultRetention result){
        String asJSON = "";
        try {
            asJSON = PolymorphicJsonMapper.serialize(result);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize database retention foroperation with id " + operationId + ". Caused by: ", e);
            logger.error("Check the engine logs for details about the failed database retention");
        }

        if (!asJSON.isEmpty()) {
            Message response = MessageBuilder.withBody(asJSON.getBytes())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbitTemplate.convertAndSend(resultExchange.getName(), AMQPConfig.RetentionResultBindingKey, response);
        }
    }

    public void produceTerminationResults() {
        //Not yet supported, but it should be...

        String asJSON = "";
        Message response = MessageBuilder.withBody(asJSON.getBytes())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        rabbitTemplate.convertAndSend(resultExchange.getName(), AMQPConfig.TerminationResultBindingKey, response);
    }
}
