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

    public void simpleLocalOperation(){
        ICommand localBlock =  dockerInterface()
                .chainCommands(this.eth0Interface())
                .chainCommands(this.localIp())
                .chainCommands(this.rxBytes())
                .chainCommands(this.txBytes())
                .addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("Completed successfully"));

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

        ExpectedOutcome resolvedOutcome = executeOperation(simpleOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    public void simpleFailingOperation(){
        ICommand localBlock =  dockerInterface()
                .chainCommands(commandWithExpectedOutcomeNotReached())
                .addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("Completed successfully"));

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

        ExpectedOutcome resolvedOutcome = executeOperation(failingOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.FAILURE);
        Assert.assertFalse(resolvedOutcome.isResolved());
    }

    public void invalidOperation(){
        ICommand localBlock =  dockerInterface()
                .chainCommands(invalidCommand())
                .addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("Completed successfully"));

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

        ExpectedOutcome resolvedOutcome = executeOperation(invalidOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.FAILURE);
        Assert.assertFalse(resolvedOutcome.isResolved());
    }

    private ExpectedOutcome executeOperation(Operation operation) throws AssertionError{
        ExpectedOutcome resolvedOutcome = null;
        try {
            Future<ExpectedOutcome> backupResult = EngineTestUtils.getQueueInstance().submit(() -> EngineTestUtils.getEngineInstance().executeOperation(operation));
            resolvedOutcome = backupResult.get();

            logger.info("Operation " + operation.getFullOperationName() + " Completed with result " + backupResult.get().getOutcome());
            logger.info("Result Message: " + backupResult.get().getMessage());
        }catch (Exception e){
            Assert.fail(e.getMessage());
        }

        return resolvedOutcome;
    }

    public void nestedBlock() {
        Block parentBlock = (Block) new Block().addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("Parent block completed successfully"));
        for (int i = 1; i <= 3; i++) {
            String blockID = "block-" + i;
            Block commandBlock = (Block)new Block()
                    .addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("block-" + i + " completed successfully"))
                    .addDynamicField("var.block.id", blockID + "-should-get-overridden");

            for (int j = 1; j <= 3; j++) {
                String commandID = "command-" + j;
                Command blockPart = blockPartCommand(blockID, commandID);
                commandBlock.addChildBlock(blockPart);
            }
            parentBlock.addChildBlock(commandBlock);
        }

        Operation blockOperation = (Operation)new Operation()
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

        ExpectedOutcome resolvedOutcome = executeOperation(blockOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    public void repeatingBlock() {
        ICommand echoValue = new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("echo $var.block.counter")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(5)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("$var.block.counter")
                                .withBinaryRelation(BinaryRelation.EQUALS)
                                .withOutcome(ResultStatus.SUCCESS)
                );
        ICommand increment = InternalCommands.assignValue("var.block.counter", "$var.block.counter + 1");

        Block repeatingBlock = ((Block)echoValue.chainCommands(increment))
                .addRepeatCondition(
                        new ExecutionCondition()
                                .withVariable("$var.block.counter")
                                .withBinaryRelation(BinaryRelation.LESSER_OR_EQUAL_TO)
                                .withExpectedValue("$var.block.repeatCount")
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

        ExpectedOutcome resolvedOutcome = executeOperation(repeatingOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    public void fileWriteOperation() {
        ICommand fileWrite = new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("cat /etc/hosts")
                .withSecondsToTimeout(90)
                .withUseRawOutput(true)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("cat[\\w\\W]*\\Q$sixsense.session.prompt.local\\E")
                                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                                .withOutcome(ResultStatus.SUCCESS)
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

        ExpectedOutcome resolvedOutcome = executeOperation(writeOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    public void drainingFileOperation(){
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

        ExpectedOutcome resolvedOutcome = executeOperation(writeOperation);
        Assert.assertEquals(resolvedOutcome.getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(resolvedOutcome.isResolved());
    }

    private ICommand dockerInterface(){
        return new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("ifconfig | grep 'docker'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("The correct interface name was found")
                                .withExpectedValue("docker")
                                .withBinaryRelation(BinaryRelation.CONTAINS)
                );
    }

    private ICommand eth0Interface(){
        return new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("ifconfig | grep 'eth0'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("The correct interface name was found")
                                .withExpectedValue("eth0")
                                .withBinaryRelation(BinaryRelation.CONTAINS)
                );
    }

    private ICommand localIp(){
        return new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("ifconfig | grep 'inet addr' | head -n 2 | tail -1")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("The correct IP address is defined for the interface")
                                .withExpectedValue("172.31.254.65")
                                .withBinaryRelation(BinaryRelation.CONTAINS)
                );
    }

    private ICommand rxBytes(){
        return new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("ifconfig eth0 | grep 'RX bytes' | awk '{print $2}' | sed 's/bytes://g'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("The correct IP address is defined for the interface")
                                .withExpectedValue("1000")
                                .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO)
                );
    }

    private ICommand txBytes(){
        return new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("ifconfig eth0 | grep 'TX bytes' | awk '{print $2}' | sed 's/bytes://g'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("The correct IP address is defined for the interface")
                                .withExpectedValue("1000")
                                .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO)
                );
    }

    private ICommand commandWithExpectedOutcomeNotReached(){
        return new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("ifconfig eth0 | grep 'TX bytes'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("Docker interface should not be here")
                                .withExpectedValue("docker")
                                .withBinaryRelation(BinaryRelation.CONTAINS)
                );
    }

    private ICommand invalidCommand(){
        return new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("dsfhjk error on purpose")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("Docker interface should not be here")
                                .withExpectedValue("docker")
                                .withBinaryRelation(BinaryRelation.CONTAINS)
                );
    }

    private ICommand drainingFileCopy(){
        return new Command()
                .withChannel(ChannelType.LOCAL)
                //.withCommandText("cat /var/log/iptables.log-20190420")
                .withCommandText("cat /var/log/BB_cluster.log")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(45)
                .withUseRawOutput(true)
                .addOutputPipe(new DrainingPipe())
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("\\n\\Q$sixsense.session.prompt.local\\E")
                                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                                .withOutcome(ResultStatus.SUCCESS)
                )
                .withSaveTo(
                        new VariableRetention()
                                .withResultRetention(ResultRetention.File)
                                .withName("iptable.daily.log")
                );
    }

    private Command blockPartCommand(String blockID, String commandID){
        return (Command)new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("echo $var.block.id $var.command.id")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .addDynamicField("var.block.id", blockID)
                .addDynamicField("var.command.id", commandID)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("Block ID has benn matched")
                                .withExpectedValue(blockID)
                                .withBinaryRelation(BinaryRelation.CONTAINS)
                )
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withOutcome(ResultStatus.SUCCESS)
                                .withMessage("Command ID has benn matched")
                                .withExpectedValue(commandID)
                                .withBinaryRelation(BinaryRelation.CONTAINS)
                )
                .withOutcomeAggregation(LogicalCondition.AND)
                .withAggregatedOutcomeMessage("Command params found in response");
    }
}
