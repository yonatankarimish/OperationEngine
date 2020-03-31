package com.SixSense;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.*;

@SpringBootApplication

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static ConfigurableApplicationContext appContext;

    public static void main(String[] args) {
        /*This has now been deprecated in favour of the OperationController.debugMethod()
         * To run remotely, enter the following command in cli (parameter order matters!):
         * java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005 -jar /sixsense/OperationEngine.jar
         * make sure your engine service is down, to avoid duplicate execution (service.engine.stop should do the trick)
         * logger will output messages above INFO level to system.out*/
        /*logger.info("SixSense Session engine demo");
        logger.info("Press any key to start tests");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        logger.info("Starting now");*/

        appContext = SpringApplication.run(Main.class, args);
    }
}