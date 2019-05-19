package com.SixSense;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Scanner;

public class FirstTest {
    private static final Logger logger = LogManager.getLogger(FirstTest.class);

    @BeforeSuite
    public void initialize(){
        logger.info("SixSense Session engine tests");
        logger.info("Press any key to start tests");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        logger.info("Starting now");
    }

    @Test
    public void FirstEngineTest(){
        logger.info("This is the first TestNG test for Operation Engine");
    }

    @Test
    public void SecondEngineTest(){
        logger.info("This is the second TestNG test for Operation Engine");
    }
}
