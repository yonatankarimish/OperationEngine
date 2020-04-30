package com.sixsense;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.*;

import java.util.Scanner;

public class DebugConfig {
    private static final Logger logger = LogManager.getLogger(DebugConfig.class);

    @BeforeSuite
    @Parameters({"environment"})
    public void initialize(@Optional("testing") String environment){
        logger.info("SixSense Session engine - " + environment + " environment");
        if(environment.equals("debug")) {
            logger.info("Press any key to start tests");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
        }
        logger.info("Starting now");
    }
}
