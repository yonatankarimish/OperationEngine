package com.sixsense.api.amqp.config;


import com.sixsense.config.HostConfig;
import com.sixsense.config.ThreadingConfig;
import com.sixsense.threading.ThreadingManager;
import com.rabbitmq.client.BlockedListener;
import com.rabbitmq.client.ShutdownSignalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*https://www.rabbitmq.com/tutorials/tutorial-four-spring-amqp.html
* Should explain the whole class, read the article through
*
* https://groups.google.com/forum/#!topic/rabbitmq-users/f6AlwP6Tcv0
* should give more insight as to some of the advanced configurations*/
@Configuration
@EnableConfigurationProperties({HostConfig.class, ThreadingConfig.class})
public class AMQPConfig {
    private static final Logger logger = LogManager.getLogger(AMQPConfig.class);
    public static final String OperationResultBindingKey = "operation";
    public static final String RetentionResultBindingKey = "retention";
    public static final String TerminationResultBindingKey = "terminate";

    private final ThreadingManager threadingManager;
    private final HostConfig.RabbitHost rabbitHost;
    private final ThreadingConfig.ThreadingProperties amqpThreadingProperties;

    @Autowired
    public AMQPConfig(ThreadingManager threadingManager, HostConfig hostConfig, ThreadingConfig threadingConfig){
        this.threadingManager = threadingManager;
        this.rabbitHost = hostConfig.getRabbit();
        this.amqpThreadingProperties = threadingConfig.getAmqp();
    }


    @Bean
    public ConnectionFactory rabbitConnectionFactory(){
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();

        //host configuration
        connectionFactory.setHost(rabbitHost.getHost());
        connectionFactory.setPort(rabbitHost.getPort());
        connectionFactory.setUsername(rabbitHost.getUsername());
        connectionFactory.setPassword(rabbitHost.getPassword());
        connectionFactory.setVirtualHost(rabbitHost.getVhost());

        //maximum idle (cached) connections and channels. Currently using default values
        connectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
        connectionFactory.setConnectionNameStrategy(nameStrategy -> "Engine-Connection");
        this.threadingManager.injectAMQPFactoryWithPool(connectionFactory);

        //enable publisher confirms
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);

        //connection and channel listeners
        connectionFactory.addConnectionListener(new ConnectionListener() {
            @Override
            public void onCreate(Connection connection) {
                logger.debug("AMQP CachingConnectionFactory opened a new connection");
                connection.addBlockedListener(new BlockedListener() {
                    @Override
                    public void handleBlocked(String reason) {
                        logger.warn("AMQP connection to broker is now blocked. Caused by: " + reason);
                    }

                    @Override
                    public void handleUnblocked() {
                        logger.info("AMQP connection to broker is now unblocked");
                    }
                });
            }

            @Override
            public void onShutDown(ShutdownSignalException signal) {
                logger.debug("AMQP CachingConnectionFactory closed a connection (" + signal.getMessage() + ")");
            }
        });
        connectionFactory.addChannelListener((channel, isTransactional) -> {
            //might extend this in the future
        });

        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(){
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setMandatory(true); //must be set to "true" for publisher confirms
        template.setUsePublisherConnection(true); //uses a separate connection(s) from separate connection factories for publishing and consuming (to avoid blocking consumers while publishing)
        template.setConfirmCallback((CorrelationData correlationData, boolean ack, String cause) -> {
            if(!ack){
                logger.error("Failed to publish message to rabbitmq broker. Caused by: " + cause);
            }
        });
        template.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey) -> {
            if(replyCode != 200){
                logger.error("Failed to route message through exchange " + exchange + " with routing key " + routingKey + ". Caused by: " + replyText);
            }
        });
        return template;
    }

    @Bean
    public DirectExchange resultsExchange(){
        return new DirectExchange("engine.results");
    }

    @Bean
    public Queue operationResultsQueue(){
        //Queue(String name, boolean durable)
        return new Queue("engine.results.operation", true);
    }

    @Bean
    public Queue retentionResultsQueue(){
        //Queue(String name, boolean durable)
        return new Queue("engine.results.retention", true);
    }

    @Bean
    public Queue terminationResultsQueue(){
        //Queue(String name, boolean durable)
        return new Queue("engine.results.terminate", true);
    }

    @Bean
    public Binding bindOperationResults(DirectExchange resultsExchange, Queue operationResultsQueue){
        return BindingBuilder.bind(operationResultsQueue).to(resultsExchange).with(OperationResultBindingKey);
    }

    @Bean
    public Binding bindRetentionResults(DirectExchange resultsExchange, Queue retentionResultsQueue){
        return BindingBuilder.bind(retentionResultsQueue).to(resultsExchange).with(RetentionResultBindingKey);
    }

    @Bean
    public Binding bindTerminationResults(DirectExchange resultsExchange, Queue terminationResultsQueue){
        return BindingBuilder.bind(terminationResultsQueue).to(resultsExchange).with(TerminationResultBindingKey);
    }
}
