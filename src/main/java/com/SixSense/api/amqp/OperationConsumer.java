package com.SixSense.api.amqp;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.commands.ParallelWorkflow;
import com.SixSense.data.devices.RawExecutionConfig;
import com.SixSense.data.retention.OperationResult;
import com.SixSense.engine.WorkflowManager;
import com.SixSense.queue.WorkerQueue;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.PolymorphicJsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class OperationConsumer {
    private static final Logger logger = LogManager.getLogger(OperationConsumer.class);

    //Engine entities
    private final WorkflowManager workflowManager;
    private final WorkerQueue workerQueue;

    //AMQP entities
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange resultExchange;

    @Autowired
    public OperationConsumer(WorkflowManager workflowManager, WorkerQueue workerQueue, RabbitTemplate rabbitTemplate, DirectExchange resultExchange) {
        this.workflowManager = workflowManager;
        this.workerQueue = workerQueue;
        this.rabbitTemplate = rabbitTemplate;
        this.resultExchange = resultExchange;
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
                    key = "execute.operation"
            )
    )
    public void executeOperation(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String operationJSON = new String(message.getBody(), StandardCharsets.UTF_8);
        String queueName = message.getMessageProperties().getConsumerQueue();
        RawExecutionConfig rawExecutionConfig;
        boolean successfullySubmitted = false;

        try {
            logger.info("Consumed message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue());
            rawExecutionConfig = PolymorphicJsonMapper.deserialize(operationJSON, RawExecutionConfig.class);
            logger.info("Generated raw execution config by deserializing message with delivery tag " + deliveryTag + " from queue " + queueName);
        } catch (JsonProcessingException e) {
            rawExecutionConfig = null;
            logger.error("Failed to deserialize message with delivery tag " + deliveryTag + " from queue " + queueName + ". Caused by: ", e);
        }

        if (rawExecutionConfig != null) {
            ParallelWorkflow workflow = CommandUtils.composeWorkflow(rawExecutionConfig);
            final RawExecutionConfig asFinalCopy = rawExecutionConfig; //this ugly line is needed because lambda expressions require final variables

            CompletableFuture<Map<String, OperationResult>> workflowResult = workflowManager.executeWorkflow(workflow);
            workerQueue.acceptAsync(workflowResult, map -> {
                produceOperationResults(asFinalCopy, workflow, map, queueName, deliveryTag);
            });

            successfullySubmitted = true;
        }

        //TODO: Manual acknowledgement should happen only after publisher confirm is received (and not after sending for submission)
        try {
            if (successfullySubmitted) {
                channel.basicAck(deliveryTag, false);
            } else {
                //basicNack(long deliveryTag, boolean multiple, boolean requeue)
                channel.basicNack(deliveryTag, false, false);
            }
        } catch (IOException e) {
            logger.error("Failed to acknowledge message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue(), e);
        }
    }

    private void produceOperationResults(RawExecutionConfig rawExecutionConfig, ParallelWorkflow composedWorkflow, Map<String, OperationResult> workflowResult, String queue, long deliveryTag) {
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
                    key = "execute.operation"
            )
    )
    public void terminateOperation(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String terminationJSON = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.info("Consumed message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue());
            logger.error("Terminating operations by AMQP is not yet supported. This should be added in a future update (delivery tag " + deliveryTag + ", queue " + message.getMessageProperties().getConsumerQueue());

            //basicNack(long deliveryTag, boolean multiple, boolean requeue)
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            logger.error("Failed to acknowledge message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue(), e);
        }
    }

    private void produceTerminationResults() {
        //Not yet supported, but it should be...
    }
}
