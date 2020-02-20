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
        appContext = SpringApplication.run(Main.class, args);
    }

    /*This has now been deprecated in favour of the OperationController.debugMethod()*/
    //To run remotely, enter the following command in cli (parameter order matters!):
    //java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005 -jar /sixsense/OperationEngine.jar
    //logger will output messages above INFO level to system.out
}