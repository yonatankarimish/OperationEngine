package com.sixsense.operation;

import com.sixsense.RemoteConfig;
import com.sixsense.SixSenseBaseTest;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.devices.Credentials;
import com.sixsense.model.logic.*;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.mocks.OperationMocks;
import com.sixsense.utillity.CommandUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

public class RemoteOperationTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(RemoteOperationTests.class);

    @Test(dataProvider = "f5BigIpConfig", dataProviderClass = RemoteConfig.class, groups = {"operation"})
    public void f5BigIpBackup(String host, String username, String password){
        Operation f5Backup = CommandUtils.composeWorkflow(OperationMocks.f5BigIpBackup(
            Collections.singletonList(
                new Credentials()
                    .withHost(host)
                    .withUsername(username)
                    .withPassword(password)
            )
        )).getParallelOperations().get(0); //We can get the first operation, since only one credential set was passed

        OperationResult operationResult = OperationTestUtils.executeOperation(f5Backup);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    @Test(dataProvider = "f5BigIpConfig", dataProviderClass = RemoteConfig.class, groups = {"operation"})
    public void f5BigIpInventory(String host, String username, String password){
        Operation f5Backup = CommandUtils.composeWorkflow(OperationMocks.f5BigIpInventory(
            Collections.singletonList(
                new Credentials()
                    .withHost(host)
                    .withUsername(username)
                    .withPassword(password)
            )
        )).getParallelOperations().get(0); //We can get the first operation, since only one credential set was passed

        OperationResult operationResult = OperationTestUtils.executeOperation(f5Backup);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }
}
