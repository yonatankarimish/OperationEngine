package com.sixsense.api.amqp;

import com.sixsense.api.ApiDebuggingAware;
import com.sixsense.api.amqp.config.AMQPConfig;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.commands.ParallelWorkflow;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.model.wrappers.RawExecutionConfig;
import com.sixsense.model.wrappers.RawTerminationConfig;
import com.sixsense.services.SessionEngine;
import com.sixsense.services.WorkflowManager;
import com.sixsense.threading.ThreadingManager;
import com.sixsense.utillity.CommandUtils;
import com.sixsense.utillity.DynamicFieldGlossary;
import com.sixsense.utillity.PolymorphicJsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class OperationConsumer extends ApiDebuggingAware {
    private static final Logger logger = LogManager.getLogger(OperationConsumer.class);

    //Engine entities
    private final WorkflowManager workflowManager;
    private final ThreadingManager threadingManager;
    private final SessionEngine sessionEngine;
    private final OperationProducer operationProducer;

    @Autowired
    public OperationConsumer(WorkflowManager workflowManager, ThreadingManager threadingManager, SessionEngine sessionEngine, OperationProducer operationProducer) {
        super();
        this.workflowManager = workflowManager;
        this.threadingManager = threadingManager;
        this.sessionEngine = sessionEngine;
        this.operationProducer = operationProducer;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(
                            value = "engine.operations",
                            type = ExchangeTypes.DIRECT
                    ),
                    value = @Queue(
                            value = "engine.operations.execute",
                            durable = "true"
                    ),
                    key = "execute"
            )
    )
    public void executeOperation(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String operationJSON = new String(message.getBody(), StandardCharsets.UTF_8);
        String queueName = message.getMessageProperties().getConsumerQueue();
        RawExecutionConfig rawExecutionConfig;

        try {
            logger.info("Consumed message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue());
            rawExecutionConfig = PolymorphicJsonMapper.deserialize(operationJSON, RawExecutionConfig.class);
            logger.info("Generated raw execution config by deserializing message with delivery tag " + deliveryTag + " from queue " + queueName);
        } catch (JsonProcessingException e) {
            /*deserializing the same faulty message twice will just throw the same exception twice
            * so we send a channel.ack() to the broker allowing it to discard the message
            * since the engine is logging the failure*/
            logger.error("Failed to deserialize message with delivery tag " + deliveryTag + " from queue " + queueName + ". Caused by: " + e.getMessage());
            AMQPConfig.acknowledgeMessage(channel, message.getMessageProperties().getConsumerQueue(), true, deliveryTag);
            rawExecutionConfig = null;
        }

        if (rawExecutionConfig != null) {
            rawExecutionConfig.setStartTime(Instant.now());
            ParallelWorkflow workflow = CommandUtils.composeWorkflow(rawExecutionConfig);
            final RawExecutionConfig asFinalCopy = rawExecutionConfig; //this ugly line is needed because lambda expressions require final variables

            workflowManager.executeWorkflow(workflow).thenApply(map -> {
                for (Operation operation : workflow.getParallelOperations()) {
                    OperationResult result = map.get(operation.getUUID());
                    asFinalCopy.addResult(operation.getDynamicFields().get(DynamicFieldGlossary.device_internal_id), result);
                }

                /* As of writing this comment, the only checked exception thrown by the operation producer is JsonProcessingException,
                 * which is highly unlikely (unless the serializer is configured wrong)
                 * If such an exception does occur, it is pointless to retry consuming a message, since it will continuously fail to serialize
                 * So we currently just return true (and ack(); the message)*/
                operationProducer.produceOperationResults(asFinalCopy, queueName, deliveryTag);
                return true;
            }).thenAccept(successful -> AMQPConfig.acknowledgeMessage(
                channel, message.getMessageProperties().getConsumerQueue(), successful, deliveryTag)
            );
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(
                            value = "engine.operations",
                            type = ExchangeTypes.DIRECT
                    ),
                    value = @Queue(
                            value = "engine.operations.terminate",
                            durable = "true"
                    ),
                    key = "terminate"
            )
    )
    public void terminateOperation(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String terminationJSON = new String(message.getBody(), StandardCharsets.UTF_8);
        String queueName = message.getMessageProperties().getConsumerQueue();
        RawTerminationConfig rawTerminationConfig;

        try {
            logger.info("Consumed message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue());
            rawTerminationConfig = new RawTerminationConfig(
                PolymorphicJsonMapper.deserialize(terminationJSON, Set.class)
            );
            logger.info("Parsed operation ids for termination by deserializing message with delivery tag " + deliveryTag + " from queue " + queueName);
        } catch (JsonProcessingException e) {
            /*deserializing the same faulty message twice will just throw the same exception twice
             * so we send a channel.ack() to the broker allowing it to discard the message
             * since the engine is logging the failure*/
            logger.error("Failed to deserialize message with delivery tag " + deliveryTag + " from queue " + queueName + ". Caused by: " + e.getMessage());
            AMQPConfig.acknowledgeMessage(channel, message.getMessageProperties().getConsumerQueue(), true, deliveryTag);
            rawTerminationConfig = null;
        }

        if (rawTerminationConfig != null) {
            rawTerminationConfig.setStartTime(Instant.now());

            Map<String, CompletableFuture<OperationResult>> futureTerminations = new HashMap<>();
            for(String operationId : rawTerminationConfig.getOperationIds()) {
                CompletableFuture<OperationResult> futureTermination = threadingManager.submit(() -> sessionEngine.terminateOperation(operationId));
                futureTerminations.put(operationId, futureTermination);
            }


            final RawTerminationConfig asFinalCopy = rawTerminationConfig; //this ugly line is needed because lambda expressions require final variables
            CompletableFuture.allOf(
                //Wait for all terminations to finish asynchronously
                futureTerminations.values().toArray(CompletableFuture[]::new)
            ).thenApply(voidStub -> {
                //map each operation id to the termination result
                for(Map.Entry<String, CompletableFuture<OperationResult>> termination : futureTerminations.entrySet()){
                    String operationId = termination.getKey();
                    asFinalCopy.addResult(operationId, termination.getValue().join()); //add to the raw config
                }

                /* As of writing this comment, the only checked exception thrown by the operation producer is JsonProcessingException,
                 * which is highly unlikely (unless the serializer is configured wrong)
                 * If such an exception does occur, it is pointless to retry consuming a message, since it will continuously fail to serialize
                 * So we currently just return true (and ack(); the message)*/
                operationProducer.produceTerminationResults(asFinalCopy, queueName, deliveryTag);
                return true;
            }).thenAccept(successful -> AMQPConfig.acknowledgeMessage(
                channel, message.getMessageProperties().getConsumerQueue(), successful, deliveryTag)
            );
        }
    }
}
