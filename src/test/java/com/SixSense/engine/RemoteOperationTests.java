package com.SixSense.engine;

import com.SixSense.RemoteConfig;
import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.devices.Device;
import com.SixSense.data.devices.VendorProductVersion;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.util.InternalCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.Future;

public class RemoteOperationTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(RemoteOperationTests.class);

    @Test(dataProvider = "f5BigIpConfig", dataProviderClass = RemoteConfig.class, groups = {"engine"})
    public void f5BigIpBackup(String host, String username, String password){
        ICommand executionBlock = new Block()
            .addChildBlock(sshConnect())
            .addChildBlock(rebuildSixSenseDirectory())
            //.addChildBlock(etcHosts())
            .addChildBlock(etcHosts(host, username, password))
            .addChildBlock(exitCommand())
            .addDynamicField("device.host", host)
            .addDynamicField("device.username", username)
            .addDynamicField("device.password", password)
            .addDynamicField("device.port", "22");

        Operation f5Backup = new Operation()
            .withDevice(new Device().withVpv(
                new VendorProductVersion()
                    .withVendor("F5")
                    .withProduct("BigIP")
                    .withVersion("11 and above")
            ))
            .withOperationName("Configuration Backup")
            .withExecutionBlock(executionBlock)
            .addChannel(ChannelType.LOCAL)
            .addChannel(ChannelType.REMOTE)
            .addChannel(ChannelType.DOWNLOAD);

        ExpressionResult resolvedOutcome = executeOperation(f5Backup);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    private ExpressionResult executeOperation(Operation operation) throws AssertionError {
        ExpressionResult resolvedOutcome = null;
        try {
            Future<ExpressionResult> backupResult = EngineTestUtils.getQueueInstance().submit(() -> EngineTestUtils.getEngineInstance().executeOperation(operation));
            resolvedOutcome = backupResult.get();

            logger.info("Operation " + operation.getFullOperationName() + " Completed with result " + resolvedOutcome.getOutcome());
            logger.info("Result Message: " + resolvedOutcome.getMessage());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        return resolvedOutcome;
    }

    private ICommand sshConnect(){
        ICommand ssh = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("ssh $device.username@$device.host -p $device.port")
            //.withMinimalSecondsToResponse(5)
            .withSecondsToTimeout(60)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("assword:")
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("connecting (yes/no)")
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("connecting")
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("REMOTE HOST IDENTIFICATION HAS CHANGE")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("SSH Key Mismatch")
                        )
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("Connection timed out")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Connection timed out")
                        )
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("Connection refused")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Connection refused")
                        )
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("No route to host")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("No route to host")
                        )
                    )
            ).withSaveTo(new VariableRetention()
                .withResultRetention(ResultRetention.Variable)
                .withName("ssh.connect.response")
            );

        ICommand rsaFirstTime = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("yes")
            //.withMinimalSecondsToResponse(5)
            .withSecondsToTimeout(60)
            .withExecutionCondition(
                new LogicalExpression<ExecutionCondition>()
                    .addResolvable(new ExecutionCondition()
                        .withVariable("$ssh.connect.response")
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("connecting (yes/no)")
                    )
            )
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("assword:")
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("denied")
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("Name or service not known")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Name or service not known")
                        )
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("REMOTE HOST IDENTIFICATION HAS CHANGE")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("SSH Key Mismatch")
                        )
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("Connection timed out")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Connection timed out")
                        )
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("Connection refused")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Connection refused")
                        )
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("No route to host")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("No route to host")
                        )
                    ).addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("Invalid argument")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Invalid argument")
                        )
                    )
            ).withSaveTo(
                new VariableRetention()
                    .withResultRetention(ResultRetention.Variable)
                    .withName("ssh.connect.response")
            );

        ICommand typePassword = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("$device.password")
            //.withMinimalSecondsToResponse(5)
            .withSecondsToTimeout(60)
            .withExecutionCondition(
                new LogicalExpression<ExecutionCondition>()
                    .addResolvable(new ExecutionCondition()
                        .withVariable("$device.password")
                        .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                        .withExpectedValue("")
                    )
            )
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("#")
                    )
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("onnection")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Connection refused")
                        )
                    )
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("enied")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Wrong username or password")
                        )
                    )
            );

        ICommand tmsh = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("tmsh modify cli preference pager disabled")
            //.withMinimalSecondsToResponse(5)
            .withSecondsToTimeout(30)
            .withUseRawOutput(true)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                        .withExpectedValue("tmsh.*\\n\\Q$sixsense.session.prompt.remote\\E")
                )
            );

        return ssh.chainCommands(rsaFirstTime)
            .chainCommands(typePassword)
            .chainCommands(InternalCommands.invalidateCurrentPrompt(ChannelType.REMOTE.name()))
            .chainCommands(tmsh);
    }

    private ICommand rebuildSixSenseDirectory(){
        ICommand deleteOldDir = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("rm -rf /var/SixSense")
            .withSecondsToTimeout(600)
            .withUseRawOutput(true)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                        .withExpectedValue("rm -rf.*\\n\\Q$sixsense.session.prompt.remote\\E")
                )
            );

        ICommand makeNewDir = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("mkdir -p /var/SixSense")
            .withSecondsToTimeout(15)
            .withUseRawOutput(true)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                        .withExpectedValue("mkdir -p.*\\n\\Q$sixsense.session.prompt.remote\\E")
                )
            );

        return deleteOldDir.chainCommands(makeNewDir);
    }

    //get file by using 'cat' command and saving to file
    /*private ICommand etcHosts(){
        return new Command()
                .withChannel(ChannelType.REMOTE)
                .withCommandText("cat /etc/hosts")
                .withSecondsToTimeout(15)
                .withUseRawOutput(true)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("cat[\\w\\W]*\\Q$sixsense.session.prompt.remote\\E")
                                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                                .withOutcome(ResultStatus.SUCCESS)
                ).withSaveTo(
                        new VariableRetention()
                        .withResultRetention(ResultRetention.File)
                        .withName("hosts.txt")
                );
    }*/

    //get file by running scp command using dedicated download channel
    private ICommand etcHosts(String host, String username, String password){
        return InternalCommands.copyFile(
            "/etc/hosts",
            "hosts.txt",
            host,
            username,
            password,
            45
        );
    }

    private ICommand exitCommand(){
        return new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("exit")
            .withSecondsToTimeout(10);
    }
}
