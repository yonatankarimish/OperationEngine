package com.sixsense.api.amqp;

import com.sixsense.api.ApiDebuggingAware;
import com.sixsense.api.amqp.config.AMQPConfig;
import com.sixsense.api.amqp.config.EngineCorrelationData;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.commands.ParallelWorkflow;
import com.sixsense.model.retention.DatabaseVariable;
import com.sixsense.model.wrappers.RawExecutionConfig;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.model.wrappers.RawTerminationConfig;
import com.sixsense.utillity.DynamicFieldGlossary;
import com.sixsense.utillity.PolymorphicJsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
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

    public void produceOperationResults(RawExecutionConfig rawExecutionConfig, String queue, long deliveryTag) {
        try {
            rawExecutionConfig.setEndTime(Instant.now());
            String asJSON = PolymorphicJsonMapper.serialize(rawExecutionConfig);

            Message response = MessageBuilder.withBody(asJSON.getBytes())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

            EngineCorrelationData correlationData = new EngineCorrelationData(
                rawExecutionConfig.getOperation().getUUID() + "-result",
                resultExchange.getName(),
                AMQPConfig.OperationResultBindingKey,
                "Operation " + rawExecutionConfig.getOperation().getShortUUID() + ", producing operation result",
                response
            );

            rabbitTemplate.convertAndSend(resultExchange.getName(), AMQPConfig.OperationResultBindingKey, response, correlationData);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize operation result for message with delivery tag " + deliveryTag + " from queue " + queue + ". Caused by: " + e.getMessage());
            logger.error("Check the engine logs for details about the operation and it's result ");
        }
    }

    public void produceRetentionResult(String operationId, DatabaseVariable result){
        try {
            String asJSON = PolymorphicJsonMapper.serialize(result);

            Message response = MessageBuilder.withBody(asJSON.getBytes())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

            EngineCorrelationData correlationData = new EngineCorrelationData(
                operationId + "-retention-"+result.getName(),
                resultExchange.getName(),
                AMQPConfig.RetentionResultBindingKey,
                "Operation " + operationId + ", producing retention value " + result.getName(),
                response
            );

            rabbitTemplate.convertAndSend(resultExchange.getName(), AMQPConfig.RetentionResultBindingKey, response, correlationData);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize database retention for operation with id " + operationId + ". Caused by: " + e.getMessage());
            logger.error("Check the engine logs for details about the failed database retention");
        }
    }

    public void produceTerminationResults(RawTerminationConfig rawTerminationConfig, String queue, long deliveryTag) {
        try {
            String asJSON = PolymorphicJsonMapper.serialize(rawTerminationConfig);

            Message response = MessageBuilder.withBody(asJSON.getBytes())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            EngineCorrelationData correlationData = new EngineCorrelationData(
                "termination-by-tag-" + deliveryTag,
                resultExchange.getName(),
                AMQPConfig.TerminationResultBindingKey,
                "Termination with delivery tag " + deliveryTag + ", terminating running operations",
                response
            );

            rabbitTemplate.convertAndSend(resultExchange.getName(), AMQPConfig.TerminationResultBindingKey, response, correlationData);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize termination results for message with delivery tag " + deliveryTag + " from queue " + queue + ". Caused by: " + e.getMessage());
            logger.error("Check the engine logs for details about the failed database retention");
        }
    }
}
