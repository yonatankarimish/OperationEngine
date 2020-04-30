package com.sixsense;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

//We might find this useful some time in the future, currently here for debugging.
public class TestListener implements ITestListener {
    private static final Logger logger = LogManager.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        logger.debug("On test start");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logger.debug("On test success");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        logger.debug("On test failure");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.debug("On test skipped");
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        logger.debug("On test FailedButWithinSuccessPercentage");
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        this.onTestFailure(result);
    }

    @Override
    public void onStart(ITestContext context) {
        logger.debug("On test context start");
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.debug("On tes context finish");
    }
}
