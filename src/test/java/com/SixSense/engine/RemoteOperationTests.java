package com.SixSense.engine;

import com.SixSense.RemoteConfig;
import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.devices.Credentials;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.OperationResult;
import com.SixSense.mocks.TestingMocks;
import com.SixSense.util.CommandUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

public class RemoteOperationTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(RemoteOperationTests.class);

    @Test(dataProvider = "f5BigIpConfig", dataProviderClass = RemoteConfig.class, groups = {"engine"})
    public void f5BigIpBackup(String host, String username, String password){
        Operation f5Backup = CommandUtils.composeWorkflow(TestingMocks.f5BigIpBackup(
            Collections.singletonList(
                new Credentials()
                    .withHost(host)
                    .withUsername(username)
                    .withPassword(password)
            )
        )).getParallelOperations().get(0); //We can get the first operation, since only one credential set was passed

        OperationResult operationResult = EngineTestUtils.executeOperation(f5Backup);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }
}
