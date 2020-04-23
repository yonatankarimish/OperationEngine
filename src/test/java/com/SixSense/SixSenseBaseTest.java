package com.SixSense;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

public class SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(SixSenseBaseTest.class);
    private static final String spacer = "==============================================";

    @BeforeMethod(alwaysRun = true)
    public void methodHeader(Method method){
        logger.info(spacer);
        logger.info("Running test method " + method.getName());
    }

    @AfterMethod(alwaysRun = true)
    public void methodFooter(ITestResult result){
        logger.info("Test method " + result.getMethod().getMethodName() + " has finished with status " + this.getStatusFromCode(result.getStatus()));
        logger.info(spacer);
    }

    private String getStatusFromCode(int statusCode){
        switch (statusCode){
            case -1: return "CREATED";
            case 1: return "SUCCESS";
            case 2: return "FAILURE";
            case 3: return "SKIP";
            case 4: return "SUCCESS_PERCENTAGE_FAILURE";
            case 16: return "STARTED";
        }
        return "UNKNOWN";
    }
}
