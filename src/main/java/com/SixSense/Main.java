package com.SixSense;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.engine.SessionEngine;
import com.SixSense.mocks.F5BigIpBackup;
import com.SixSense.mocks.OperationMocks;
import com.SixSense.queue.WorkerQueue;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Scanner;
import java.util.concurrent.Future;

@SpringBootApplication
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    //To run remotely, enter the following command in cli (parameter order matters!):
    //java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /tmp/SixSense/OperatingSystem.jar
    //logger will output messages above INFO level to system.out
    public static void main(String[] args) {
        ConfigurableApplicationContext appContext = SpringApplication.run(Main.class, args);
        logger.info("SixSense Session engine demo");
        logger.info("Press any key to start tests");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        logger.info("Starting now");

        try {
            SessionEngine engineInstance = (SessionEngine)appContext.getBean("sessionEngine");
            WorkerQueue queueInstance = (WorkerQueue)appContext.getBean("workerQueue");
            //Operation operation = OperationMocks.simpleLocalOperation();

            //Operation operation = OperationMocks.simpleFailingOperation();

            //Operation operation = OperationMocks.erroneousOperation();

            //Operation operation = OperationMocks.nestedBlock();

            //Operation operation = OperationMocks.repeatingBlock(5);

            //Operation operation = OperationMocks.fileWriteOperation();

            //Operation operation = OperationMocks.drainingFileOperation();

            //Operation operation = F5BigIpBackup.f5BigIpBackup("172.31.254.66", "root", "password");
            Operation operation = F5BigIpBackup.f5BigIpBackup("172.31.252.179", "root", "qwe123");

            Future<ExpectedOutcome> backupResult = queueInstance.submit(() -> engineInstance.executeOperation(operation));
            System.out.println("Operation " + operation.getFullOperationName() + " Completed with result " + backupResult.get().getOutcome());
            System.out.println("Result Message: " + backupResult.get().getMessage());

            /*Future<ExpectedOutcome> backupResult = queueInstance.submit(() -> engineInstance.executeOperation(operation));
            Future<ExpectedOutcome> backupResult2 = queueInstance.submit(() -> engineInstance.executeOperation(operation2));
            System.out.println("Operation " + operation.getFullOperationName() + " Completed with result " + backupResult.get().getOutcome());
            System.out.println("Result Message: " + backupResult.get().getMessage());
            System.out.println("Operation " + operation2.getFullOperationName() + " Completed with result " + backupResult2.get().getOutcome());
            System.out.println("Result Message: " + backupResult2.get().getMessage());*/
        } catch (Exception e) {
            logger.error("A fatal exception was encountered - applications is closing now", e);
        }

        SpringApplication.exit(appContext);
    }
}
