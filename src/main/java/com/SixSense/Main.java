package com.SixSense;

import com.SixSense.data.commands.*;
import com.SixSense.data.devices.Credentials;
import com.SixSense.data.devices.RawExecutionConfig;
import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.engine.WorkflowManager;
import com.SixSense.mocks.TestingMocks;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.PolymorphicJsonMapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.*;

@SpringBootApplication
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    //To run remotely, enter the following command in cli (parameter order matters!):
    //java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005 -jar /sixsense/OperatingSystem.jar
    //logger will output messages above INFO level to system.out
    public static void main(String[] args) {
        ConfigurableApplicationContext appContext = SpringApplication.run(Main.class, args);
        logger.info("SixSense Session engine demo");
        logger.info("Press any key to start tests");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        logger.info("Starting now");

        try {
            /*logger.info(PolymorphicJsonMapper.serialize(TestingMocks.f5BigIpBackup(
                Collections.singletonList(
                    new Credentials()
                        .withHost("172.31.252.179")
                        .withUsername("root")
                        .withPassword("qwe123")
                )
            )));*/

            WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");
            RawExecutionConfig rawExecutionConfig = PolymorphicJsonMapper.deserialize(TestingMocks.rawExecutionConfigJsonString(), RawExecutionConfig.class);
            ParallelWorkflow workflow = CommandUtils.composeWorkflow(rawExecutionConfig);

            Map<String, ExpressionResult> workflowResult = wfManager.executeWorkflow(workflow).join();  //key: operation id, value: operation result
            for(Operation operation : workflow.getParallelOperations()) {
                ExpressionResult result = workflowResult.get(operation.getUUID());
                rawExecutionConfig.addResult(operation.getDynamicFields().get("device.internal.id"), result);
                logger.info("Operation(s) " + operation.getOperationName() + " Completed with result " + result.getOutcome());
                logger.info("Result Message: " + result.getMessage());
            }
            logger.info("Results added to serializable configuration");
        } catch (Exception e) {
            logger.error("A fatal exception was encountered - applications is closing now", e);
        }

        SpringApplication.exit(appContext);
    }
}