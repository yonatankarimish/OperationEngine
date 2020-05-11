package com.sixsense.api.amqp.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;

public class EngineCorrelationData extends CorrelationData {
    private volatile String exchangeName;
    private volatile String bindingKey;
    private volatile String logText;

    public EngineCorrelationData(String correlationId, String exchangeName, String bindingKey, String logText, Message message) {
        super(correlationId);
        this.exchangeName = exchangeName;
        this.bindingKey = bindingKey;
        this.logText = logText;
        this.setReturnedMessage(message);
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public String getLogText() {
        return logText;
    }
}
