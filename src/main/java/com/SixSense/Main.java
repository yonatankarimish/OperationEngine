package com.SixSense;

import com.SixSense.data.outcomes.ExpectedOutcome;
import com.SixSense.data.commands.Operation;
import com.SixSense.engine.SessionEngine;
import com.SixSense.mocks.F5BigIpBackup;
import com.SixSense.mocks.OperationMocks;
import org.apache.log4j.Logger;

import java.util.Scanner;

public class Main {
    private static Logger logger = Logger.getLogger(Main.class);
    //To run remotely, enter the following command in cli (parameter order matters!):
    //java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /tmp/SixSense/OperatingSystem.jar -Dlog4j.configuration=file:/tmp/SixSense/config/log4j.properties
    public static void main(String[] args) throws Exception{
        logger.info("SixSense Session engine demo started.");
        System.out.println("SixSense Session engine demo");
        System.out.println("Press any key to start tests");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        System.out.println("Starting now");

        SessionEngine sessionEngine = SessionEngine.getInstance();

        /*Operation simpleLocalBlock = OperationMocks.simpleLocalOperation();
        ExpectedOutcome simpleLocalBlockOperationResult = sessionEngine.executeOperation(simpleLocalBlock);
        System.out.println("Operation " + simpleLocalBlock.getFullOperationName() + " Completed with result " + simpleLocalBlockOperationResult.getOutcome());
        System.out.println("Result Message: " + simpleLocalBlockOperationResult.getMessage());

        Operation simpleFailingBlock = OperationMocks.simpleFailingOperation();
        ExpectedOutcome simpleFailingBlockOperationResult = sessionEngine.executeOperation(simpleFailingBlock);
        System.out.println("Operation " + simpleFailingBlock.getFullOperationName() + " Completed with result " + simpleFailingBlockOperationResult.getOutcome());
        System.out.println("Result Message: " + simpleFailingBlockOperationResult.getMessage());*/

        /*Operation blockTesting = OperationMocks.nestedBlock();
        ExpectedOutcome blockTestingOperationResult = sessionEngine.executeOperation(blockTesting);
        System.out.println("Operation " + blockTesting.getFullOperationName() + " Completed with result " + blockTestingOperationResult.getOutcome());
        System.out.println("Result Message: " + blockTestingOperationResult.getMessage());*/

        Operation f5BigIpBackup = F5BigIpBackup.f5BigIpBackup("172.31.252.179", "root", "default");
        ExpectedOutcome backupResult = sessionEngine.executeOperation(f5BigIpBackup);
        System.out.println("Operation " + f5BigIpBackup.getFullOperationName() + " Completed with result " + backupResult.getOutcome());
        System.out.println("Result Message: " + backupResult.getMessage());

        sessionEngine.close();


    }
}
