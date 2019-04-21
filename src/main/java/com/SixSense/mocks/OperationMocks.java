package com.SixSense.mocks;

import com.SixSense.data.devices.Device;
import com.SixSense.data.devices.VendorProductVersion;
import com.SixSense.data.logic.*;
import com.SixSense.data.commands.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.util.InternalCommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperationMocks {
    public static Operation simpleLocalOperation(){
        ICommand localBlock =  dockerInterface()
                .chainCommands(eth0Interface())
                .chainCommands(localIp())
                .chainCommands(rxBytes())
                .chainCommands(txBytes())
                .addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("Completed successfully"));

        return new Operation()
                .withDevice(new Device().withVpv(
                        new VendorProductVersion()
                                .withVendor("Linux")
                                .withProduct("Generic")
                                .withVersion("Centos 6")
                ))
                .withOperationName("Network Details - Simple")
                .withExecutionBlock(localBlock);
    }

    public static Operation simpleFailingOperation(){
        ICommand localBlock =  dockerInterface()
                .chainCommands(commandWithExpectedOutcomeNotReached())
                .addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("Completed successfully"));

        return new Operation()
                .withDevice(new Device().withVpv(
                        new VendorProductVersion()
                                .withVendor("Linux")
                                .withProduct("Generic")
                                .withVersion("Centos 6")
                ))
                .withOperationName("Network Details - Planned failure")
                .withExecutionBlock(localBlock);
    }

    public static Operation nestedBlock() {
        Block parentBlock = (Block) new Block().addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("Parent block completed successfully"));
        for (int i = 1; i <= 3; i++) {
            String blockID = "block-" + i;
            Block commandBlock = (Block)new Block()
                    .addExpectedOutcome(ExpectedOutcome.defaultOutcome().withMessage("block-" + i + " completed successfully"))
                    .addDynamicField("var.block.id", blockID + "-should-get-overridden");

            for (int j = 1; j <= 3; j++) {
                String commandID = "command-" + j;
                Command blockPart = blockPartCommand(blockID, commandID);
                commandBlock.addCommand(blockPart);
            }
            parentBlock.addBlock(commandBlock);
        }

        return (Operation) new Operation()
                .withDevice(new Device().withVpv(
                        new VendorProductVersion()
                                .withVendor("Linux")
                                .withProduct("Generic")
                                .withVersion("Centos 6")
                ))
                .withOperationName("Block testing - three nested blocks, three commands each")
                .withExecutionBlock(parentBlock)
                .addDynamicField("var.operation.name", "Three nested blocks")
                .addDynamicField("var.operation.vendor", "Linux")
                .addDynamicField("var.operation.product", "CentOS")
                .addDynamicField("var.operation.version", "6");
    }

    public static Operation repeatingBlock(int repeatCount) {
        ICommand echoValue = new Command()
                .withCommandType(CommandType.LOCAL)
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

        return (Operation) new Operation()
                .withDevice(new Device().withVpv(
                        new VendorProductVersion()
                                .withVendor("Linux")
                                .withProduct("Generic")
                                .withVersion("Centos 6")
                ))
                .withOperationName("Block testing - repeating block")
                .withExecutionBlock(repeatingBlock)
                .addDynamicField("var.block.repeatCount", String.valueOf(repeatCount))
                .addDynamicField("var.block.counter", "1");
    }

    public static Operation fileWriteOperation() {
        ICommand fileWrite = new Command()
                .withCommandType(CommandType.LOCAL)
                .withCommandText("cat /etc/hosts")
                .withSecondsToTimeout(90)
                .withUseRawOutput(true)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("cat[\\w\\W]*\\Q$sixsense.session.localPrompt\\E")
                                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                                .withOutcome(ResultStatus.SUCCESS)
                ).withSaveTo(
                        new VariableRetention()
                                .withResultRetention(ResultRetention.File)
                                .withName("hosts.txt")
                );

        return (Operation) new Operation()
                .withDevice(new Device().withVpv(
                        new VendorProductVersion()
                                .withVendor("Linux")
                                .withProduct("Generic")
                                .withVersion("Centos 6")
                ))
                .withOperationName("Network details - Write to file")
                .withExecutionBlock(fileWrite);
    }

    private static ICommand dockerInterface(){
        return new Command()
                .withCommandType(CommandType.LOCAL)
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

    private static ICommand eth0Interface(){
        return new Command()
                .withCommandType(CommandType.LOCAL)
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

    private static ICommand localIp(){
        return new Command()
                .withCommandType(CommandType.LOCAL)
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

    private static ICommand rxBytes(){
        return new Command()
                .withCommandType(CommandType.LOCAL)
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

    private static ICommand txBytes(){
        return new Command()
                .withCommandType(CommandType.LOCAL)
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

    private static ICommand commandWithExpectedOutcomeNotReached(){
        return new Command()
                .withCommandType(CommandType.LOCAL)
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

    private static Command blockPartCommand(String blockID, String commandID){
        return (Command)new Command()
                .withCommandType(CommandType.LOCAL)
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
