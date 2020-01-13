package com.SixSense.engine;

import com.SixSense.RemoteConfig;
import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.logic.*;
import com.SixSense.mocks.TestingMocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Future;

public class RemoteOperationTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(RemoteOperationTests.class);

    @Test(dataProvider = "f5BigIpConfig", dataProviderClass = RemoteConfig.class, groups = {"engine"})
    public void f5BigIpBackup(String host, String username, String password){
        Operation executionBlock = TestingMocks.f5BigIpBackup(host, username, password);
        ExpressionResult resolvedOutcome = EngineTestUtils.executeOperation(executionBlock);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }
}