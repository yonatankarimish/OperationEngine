package com.SixSense;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.engine.SessionEngine;
import com.SixSense.mocks.F5BigIpBackup;
import com.SixSense.mocks.OperationMocks;
import com.SixSense.queue.WorkerQueue;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Scanner;

@SpringBootApplication
public class Main {
    private static Logger logger = Logger.getLogger(Main.class);

    //To run remotely, enter the following command in cli (parameter order matters!):
    //java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /tmp/SixSense/OperatingSystem.jar -Dlog4j.configuration=file:/tmp/SixSense/config/log4j.properties
    public static void main(String[] args) {
        ConfigurableApplicationContext appContext = SpringApplication.run(Main.class, args);
        logger.info("SixSense Session engine demo started.");
        System.out.println("SixSense Session engine demo");
        System.out.println("Press any key to start tests");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        System.out.println("Starting now");

        try {
            SessionEngine engineInstance = (SessionEngine)appContext.getBean("sessionEngine");
            WorkerQueue queueInstance = (WorkerQueue)appContext.getBean("workerQueue");
            //Operation operation = OperationMocks.simpleLocalOperation();

            //Operation operation = OperationMocks.simpleFailingOperation();

            //Operation operation = OperationMocks.nestedBlock();

            //Operation operation = OperationMocks.repeatingBlock(5);

            //Operation operation = F5BigIpBackup.f5BigIpBackup("172.31.254.66", "root", "password");
            Operation operation = F5BigIpBackup.f5BigIpBackup("172.31.252.179", "root", "qwe123");

            ExpectedOutcome backupResult = queueInstance.submit(() -> engineInstance.executeOperation(operation)).get();
            System.out.println("Operation " + operation.getFullOperationName() + " Completed with result " + backupResult.getOutcome());
            System.out.println("Result Message: " + backupResult.getMessage());
        } catch (Exception e) {
            logger.error("A fatal exception was encountered - applications is closing now", e);
        }

        SpringApplication.exit(appContext);
    }
}
