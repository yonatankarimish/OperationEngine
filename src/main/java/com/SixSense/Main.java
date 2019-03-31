package com.SixSense;

import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Operation;
import com.SixSense.engine.SessionEngine;
import com.SixSense.mocks.OperationMocks;
import com.SixSense.util.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    private static Logger logger = Logger.getLogger(Main.class);
    //To run remotely, enter the following command in cli (parameter order matters!):
    //java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /tmp/SixSense/OperatingSystem.jar -Dlog4j.configuration=file:/tmp/SixSense/config/log4j.properties
    public static void main(String[] args) {
        logger.info("SixSense Session engine demo started.");
        System.out.println("SixSense Session engine demo");
        System.out.println("Press any key to start tests");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        System.out.println("Starting now");

        SessionEngine sessionEngine = SessionEngine.getInstance();
        Operation simpleLocalBlock = OperationMocks.simpleLocalOperation();
        ExpectedOutcome operationResult = sessionEngine.executeOperation(simpleLocalBlock);

        sessionEngine.close();
        System.out.println("Operation " + simpleLocalBlock.getFullOperationName() + " Completed with result " + operationResult.getOutcome());
        System.out.println("Result Message: " + operationResult.getMessage());

    }
}
