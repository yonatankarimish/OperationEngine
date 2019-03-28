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
    //java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /tmp/SixSense/OperatingSystem.jar -Dlog4j.configuration=file:/tmp/SixSense/configlog4j.properties
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

        //Create a new instance of an OperatingSystem wrapper
        /*OperatingSystem os = new OperatingSystem();

        try {
            //Execute a simple command, and print the results
            //OperationResult dateResult = os.runCommand("date;");
            //System.out.println(dateResult.getOutput().get(0));

            //Execute a sequence of commands in order, and print their results
            List<String> getAccessRules = new ArrayList<>();
            //getAccessRules.add("/sbin/iptables -L INPUT -n --line-numbers |  sed 's/--//g' | sed -r 's/(\\s+)/|/'  | sed 's/  \\+/|/g';");
            //getAccessRules.add("/sbin/ip6tables -L INPUT -n --line-numbers |  sed 's/--//g' | sed -r 's/(\\s+)/|/'  | sed 's/  \\+/|/g';");
            //getAccessRules.add("ifconfig;");
            getAccessRules.add("watch -n1 date;");
            getAccessRules.add("/sbin/iptables -L INPUT -n --line-numbers |  sed 's/--//g' | sed -r 's/(\\s+)/|/'  | sed 's/  \\+/|/g';");
            OperationResult accessResult = os.runScript(getAccessRules);

            for(String rule : accessResult.getOutput()) {
                System.out.println(rule);
            }
        }catch (Exception e){
            e.printStackTrace();
        }*/
    }
}
