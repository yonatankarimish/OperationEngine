package com.sixsense.api.amqp;

import com.sixsense.api.ApiDebuggingAware;
import com.sixsense.model.commands.ParallelWorkflow;
import com.sixsense.model.devices.RawExecutionConfig;
import com.sixsense.services.WorkflowManager;
import com.sixsense.utillity.CommandUtils;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OperationConsumer extends ApiDebuggingAware {
    private static final Logger logger = LogManager.getLogger(OperationConsumer.class);

    //Engine entities
    private final WorkflowManager workflowManager;
    private final OperationProducer operationProducer;

    @Autowired
    public OperationConsumer(WorkflowManager workflowManager, OperationProducer operationProducer) {
        super();
        this.workflowManager = workflowManager;
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
            rawExecutionConfig = null;
            logger.error("Failed to deserialize message with delivery tag " + deliveryTag + " from queue " + queueName + ". Caused by: " + e.getMessage());
        }

        if (rawExecutionConfig != null) {
            ParallelWorkflow workflow = CommandUtils.composeWorkflow(rawExecutionConfig);
            final RawExecutionConfig asFinalCopy = rawExecutionConfig; //this ugly line is needed because lambda expressions require final variables

            workflowManager.executeWorkflow(workflow).thenApply(map -> {
                operationProducer.produceOperationResults(asFinalCopy, workflow, map, queueName, deliveryTag);
                /* As of writing this comment, the only checked exception thrown by the operation producer is JsonProcessingException,
                 * which is highly unlikely (unless the serializer is configured wrong)
                 * If such an exception does occur, it is pointless to retry consuming a message, since it will continuously fail to serialize
                 * So we currently just return true (and ack(); the message)*/
                return true;
            }).thenAccept(successful -> {
                try {
                    if (successful) {
                        //basicAck(long deliveryTag, boolean multiple)
                        channel.basicAck(deliveryTag, false);
                    } else {
                        //basicNack(long deliveryTag, boolean multiple, boolean requeue)
                        channel.basicNack(deliveryTag, false, false);
                    }
                } catch (IOException e) {
                    logger.error("Failed to acknowledge message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue() + ". Caused by: " + e.getMessage());
                }
            });
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
        try {
            String terminationJSON = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.info("Consumed message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue());
            logger.error("Terminating operations by AMQP is not yet supported. This should be added in a future update (delivery tag " + deliveryTag + ", queue " + message.getMessageProperties().getConsumerQueue());

            //basicNack(long deliveryTag, boolean multiple, boolean requeue)
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            logger.error("Failed to acknowledge message with delivery tag " + deliveryTag + " from queue " + message.getMessageProperties().getConsumerQueue() + ". Caused by: " + e.getMessage());
        }
    }
}
