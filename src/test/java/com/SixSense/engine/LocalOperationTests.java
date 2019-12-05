package com.SixSense.engine;

import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.devices.Device;
import com.SixSense.data.devices.VendorProductVersion;
import com.SixSense.data.logic.*;
import com.SixSense.data.pipes.DrainingPipe;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.util.InternalCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Future;

@Test(groups = {"engine"})
public class LocalOperationTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(LocalOperationTests.class);

    public void simpleLocalOperation() {
        ICommand localBlock = loopbackInterface()
            .chainCommands(this.localIp())
            .chainCommands(this.rxBytes())
            .chainCommands(this.txBytes())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withExpressionResult(new ExpressionResult().withMessage("Completed successfully"))
            );

        Operation simpleOperation = new Operation()
            .withDevice(new Device().withVpv(
                new VendorProductVersion()
                    .withVendor("Linux")
                    .withProduct("Generic")
                    .withVersion("Centos 6")
            ))
            .withOperationName("Network Details - Simple")
            .withExecutionBlock(localBlock)
            .addChannel(ChannelType.LOCAL);

        ExpressionResult resolvedOutcome = executeOperation(simpleOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    public void simpleFailingOperation() {
        ICommand localBlock = loopbackInterface()
            .chainCommands(commandWithExpectedOutcomeNotReached())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withExpressionResult(new ExpressionResult().withMessage("Completed successfully"))
            );

        Operation failingOperation = new Operation()
            .withDevice(new Device().withVpv(
                new VendorProductVersion()
                    .withVendor("Linux")
                    .withProduct("Generic")
                    .withVersion("Centos 6")
            ))
            .withOperationName("Network Details - Planned failure")
            .withExecutionBlock(localBlock)
            .addChannel(ChannelType.LOCAL);

        ExpressionResult resolvedOutcome = executeOperation(failingOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.FAILURE);
        Assert.assertFalse(resolvedOutcome.isResolved());
    }

    public void invalidOperation() {
        ICommand localBlock = loopbackInterface()
            .chainCommands(invalidCommand())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withExpressionResult(new ExpressionResult().withMessage("Completed successfully"))
            );

        Operation invalidOperation = new Operation()
            .withDevice(new Device().withVpv(
                new VendorProductVersion()
                    .withVendor("Linux")
                    .withProduct("Generic")
                    .withVersion("Centos 6")
            ))
            .withOperationName("Network Details - Planned failure")
            .withExecutionBlock(localBlock)
            .addChannel(ChannelType.LOCAL);

        ExpressionResult resolvedOutcome = executeOperation(invalidOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.FAILURE);
        Assert.assertFalse(resolvedOutcome.isResolved());
    }

    public void nestedBlock() {
        Block parentBlock = (Block) new Block().withExpectedOutcome(
            new LogicalExpression<ExpectedOutcome>()
                .withExpressionResult(new ExpressionResult().withMessage("Parent block completed successfully"))
        );

        for (int i = 1; i <= 3; i++) {
            String blockID = "block-" + i;
            Block commandBlock = (Block) new Block()
                .addDynamicField("var.block.id", blockID + "-should-get-overridden")
                .withExpectedOutcome(
                    new LogicalExpression<ExpectedOutcome>()
                        .withExpressionResult(new ExpressionResult().withMessage("block-" + i + " completed successfully"))
                );

            for (int j = 1; j <= 3; j++) {
                String commandID = "command-" + j;
                Command blockPart = blockPartCommand(blockID, commandID);
                commandBlock.addChildBlock(blockPart);
            }
            parentBlock.addChildBlock(commandBlock);
        }

        Operation blockOperation = (Operation) new Operation()
            .withDevice(new Device().withVpv(
                new VendorProductVersion()
                    .withVendor("Linux")
                    .withProduct("Generic")
                    .withVersion("Centos 6")
            ))
            .withOperationName("Block testing - three nested blocks, three commands each")
            .withExecutionBlock(parentBlock)
            .addChannel(ChannelType.LOCAL)
            .addDynamicField("var.operation.name", "Three nested blocks")
            .addDynamicField("var.operation.vendor", "Linux")
            .addDynamicField("var.operation.product", "CentOS")
            .addDynamicField("var.operation.version", "6");

        ExpressionResult resolvedOutcome = executeOperation(blockOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    public void repeatingBlock() {
        ICommand echoValue = new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo $var.block.counter")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(5)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.EQUALS)
                        .withExpectedValue("$var.block.counter")
                )
            );
        ICommand increment = InternalCommands.assignValue("var.block.counter", "$var.block.counter + 1");

        Block repeatingBlock = ((Block) echoValue.chainCommands(increment))
            .withRepeatCondition(
                new LogicalExpression<ExecutionCondition>().addResolvable(
                    new ExecutionCondition()
                        .withVariable("$var.block.counter")
                        .withBinaryRelation(BinaryRelation.LESSER_OR_EQUAL_TO)
                        .withExpectedValue("$var.block.repeatCount")
                )
            );

        Operation repeatingOperation = (Operation) new Operation()
            .withDevice(new Device().withVpv(
                new VendorProductVersion()
                    .withVendor("Linux")
                    .withProduct("Generic")
                    .withVersion("Centos 6")
            ))
            .withOperationName("Block testing - repeating block")
            .withExecutionBlock(repeatingBlock)
            .addChannel(ChannelType.LOCAL)
            .addDynamicField("var.block.repeatCount", "5")
            .addDynamicField("var.block.counter", "1");

        ExpressionResult resolvedOutcome = executeOperation(repeatingOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    public void fileWriteOperation() {
        ICommand fileWrite = new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("cat /etc/hosts")
            .withSecondsToTimeout(90)
            .withUseRawOutput(true)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                        .withExpectedValue("cat[\\w\\W]*\\Q$sixsense.session.prompt.local\\E")
                )
            ).withSaveTo(
                new VariableRetention()
                    .withResultRetention(ResultRetention.File)
                    .withName("hosts.txt")
            );

        Operation writeOperation = new Operation()
            .withDevice(new Device().withVpv(
                new VendorProductVersion()
                    .withVendor("Linux")
                    .withProduct("Generic")
                    .withVersion("Centos 6")
            ))
            .withOperationName("Network details - Write to file")
            .withExecutionBlock(fileWrite)
            .addChannel(ChannelType.LOCAL);

        ExpressionResult resolvedOutcome = executeOperation(writeOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    public void drainingFileOperation() {
        Operation writeOperation = new Operation()
            .withDevice(new Device().withVpv(
                new VendorProductVersion()
                    .withVendor("Linux")
                    .withProduct("Generic")
                    .withVersion("Centos 6")
            ))
            .withOperationName("Network Details - File parsing")
            .withExecutionBlock(drainingFileCopy())
            .addChannel(ChannelType.LOCAL)
            .addChannel(ChannelType.DOWNLOAD);

        ExpressionResult resolvedOutcome = executeOperation(writeOperation);
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

    //TODO: find a way to generalize these tests for each qa machine
    private ICommand loopbackInterface() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig | grep 'Loopback'")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("Loopback")
                        .withExpressionResult(new ExpressionResult().withMessage("The correct interface name was found"))
                )
            );
    }

    private ICommand localIp() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig | grep 'inet addr' | head -n 2 | tail -1")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("127.0.0.1")
                        .withExpressionResult(new ExpressionResult().withMessage("The correct IP address is defined for the interface"))
                )
            );
    }

    private ICommand rxBytes() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig lo | grep 'RX bytes' | awk '{print $2}' | sed 's/bytes://g'")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO)
                        .withExpectedValue("1000")
                        .withExpressionResult(new ExpressionResult().withMessage("More than 1kb was sent through this interface"))
                )
            );
    }

    private ICommand txBytes() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig lo | grep 'TX bytes' | awk '{print $2}' | sed 's/bytes://g'")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO)
                        .withExpectedValue("1000")
                        .withExpressionResult(new ExpressionResult().withMessage("More than 1kb was received through this interface"))
                )
            );
    }

    private ICommand commandWithExpectedOutcomeNotReached() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig lo | grep 'TX bytes'")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("notPartOfCat")
                        .withExpressionResult(new ExpressionResult().withMessage("no byte count should not be here"))
                )
            );
    }

    private ICommand invalidCommand() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("dsfhjk error on purpose")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("docker")
                        .withExpressionResult(new ExpressionResult().withMessage("no valid response should be here"))
                )
            );
    }

    private ICommand drainingFileCopy() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            //.withCommandText("cat /var/log/iptables.log-20190420")
            .withCommandText("cat /var/log/BB_cluster.log")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(45)
            .withUseRawOutput(true)
            .addOutputPipe(new DrainingPipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                        .withExpectedValue("\\n\\Q$sixsense.session.prompt.local\\E")
                )
            )
            .withSaveTo(
                new VariableRetention()
                    .withResultRetention(ResultRetention.File)
                    .withName("iptable.daily.log")
            );
    }

    private Command blockPartCommand(String blockID, String commandID) {
        return (Command) new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo $var.block.id $var.command.id")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .addDynamicField("var.block.id", blockID)
            .addDynamicField("var.command.id", commandID)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue(blockID)
                        .withExpressionResult(new ExpressionResult().withMessage("Block ID has been matched"))
                    )
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue(commandID)
                        .withExpressionResult(new ExpressionResult().withMessage("Command ID has been matched"))
                    )
                    .withExpressionResult(new ExpressionResult().withMessage("Command params found in response"))
            );
    }
}
