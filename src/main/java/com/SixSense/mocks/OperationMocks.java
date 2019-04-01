package com.SixSense.mocks;

import com.SixSense.data.outcomes.*;
import com.SixSense.data.commands.*;

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
                .chainCommands(txBytes());

        ExpectedOutcome defaultOutcome = ExpectedOutcome.defaultOutcome()
                .withMessage("Completed successfully");

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(defaultOutcome);
        localBlock.setExpectedOutcomes(expectedOutcomes);

        return new Operation()
                .withVPV("Linux generic centos6")
                .withOperationName("Network Details - Simple")
                .withExecutionBlock(localBlock);
    }

    public static Operation simpleFailingOperation(){
        ICommand localBlock =  dockerInterface()
                .chainCommands(commandWithExpectedOutcomeNotReached());

        ExpectedOutcome defaultOutcome = ExpectedOutcome.defaultOutcome()
                .withMessage("Completed successfully");

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(defaultOutcome);
        localBlock.setExpectedOutcomes(expectedOutcomes);

        return new Operation()
                .withVPV("Linux generic centos6")
                .withOperationName("Network Details - Planned failure")
                .withExecutionBlock(localBlock);
    }

    public static Operation nestedBlock(){
        Block parentBlock = new Block();
        for(int i=1; i<=3; i++){
            String blockID = "block-"+i;
            Block commandBlock = new Block();
            ExpectedOutcome blockOutcome = ExpectedOutcome.defaultOutcome().withMessage("block-"+i+" completed successfully");

            Map<String, String> dynamicFields = new HashMap<>();
            dynamicFields.put("\\$var.block.id", blockID+"-should-get-overridden");
            commandBlock.addDynamicFields(dynamicFields);

            List<ExpectedOutcome> blockOutcomes = new ArrayList<>();
            blockOutcomes.add(blockOutcome);
            parentBlock.setExpectedOutcomes(blockOutcomes);

            for(int j=1; j<=3; j++){
                String commandID = "command-"+j;
                ICommand blockPart =  blockPartCommand(blockID, commandID);
                commandBlock.addCommands(blockPart);
            }
            parentBlock.addCommands(commandBlock);
        }

        Map<String, String> operationDynamicFields = new HashMap<>();
        operationDynamicFields.put("\\$var.operation.name", "Three nested blocks");
        operationDynamicFields.put("\\$var.operation.vendor", "Linux");
        operationDynamicFields.put("\\$var.operation.product", "CentOS");
        operationDynamicFields.put("\\$var.operation.version", "6");

        ExpectedOutcome defaultOutcome = ExpectedOutcome.defaultOutcome().withMessage("Parent block completed successfully");
        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(defaultOutcome);
        parentBlock.setExpectedOutcomes(expectedOutcomes);

        Operation operation =  new Operation()
                .withVPV("Linux generic centos6")
                .withOperationName("Block testing - three nested blocks, three commands each")
                .withExecutionBlock(parentBlock);
        operation.addDynamicFields(operationDynamicFields);
        return operation;
    }

    public static ICommand sshConnect(String remoteHost, String username, String password){
        return null;
    }


    public static ICommand dockerInterface(){
        Command command = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ifconfig | grep 'docker'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10);

        ExpectedOutcome shouldContainDockerInterface = new ExpectedOutcome()
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("The correct interface name was found")
                .withExpectedOutput("docker")
                .withBinaryRelation(BinaryRelation.CONTAINS);

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(shouldContainDockerInterface);
        command.setExpectedOutcomes(expectedOutcomes);

        return command;
    }

    public static ICommand eth0Interface(){
        Command command = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ifconfig | grep 'eth0'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10);

        ExpectedOutcome shouldContainEth0 = new ExpectedOutcome()
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("The correct interface name was found")
                .withExpectedOutput("eth0")
                .withBinaryRelation(BinaryRelation.CONTAINS);

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(shouldContainEth0);
        command.setExpectedOutcomes(expectedOutcomes);

        return command;
    }

    public static ICommand localIp(){
        Command command = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ifconfig | grep 'inet addr' | head -n 2 | tail -1")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10);

        ExpectedOutcome shouldContainLocalIp = new ExpectedOutcome()
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("The correct IP address is defined for the interface")
                .withExpectedOutput("172.31.254.65")
                .withBinaryRelation(BinaryRelation.CONTAINS);

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(shouldContainLocalIp);
        command.setExpectedOutcomes(expectedOutcomes);

        return command;
    }

    public static ICommand rxBytes(){
        Command command = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ifconfig eth0 | grep 'RX bytes' | awk '{print $2}' | sed  's/bytes://g'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10);

        ExpectedOutcome shouldContainLocalIp = new ExpectedOutcome()
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("The correct IP address is defined for the interface")
                .withExpectedOutput("1000")
                .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO);

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(shouldContainLocalIp);
        command.setExpectedOutcomes(expectedOutcomes);

        return command;
    }

    public static ICommand txBytes(){
        Command command = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ifconfig eth0 | grep 'TX bytes' | awk '{print $2}' | sed  's/bytes://g'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10);

        ExpectedOutcome shouldContainLocalIp = new ExpectedOutcome()
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("The correct IP address is defined for the interface")
                .withExpectedOutput("1000")
                .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO);

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(shouldContainLocalIp);
        command.setExpectedOutcomes(expectedOutcomes);

        return command;
    }

    public static ICommand commandWithExpectedOutcomeNotReached(){
        Command command = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ifconfig eth0 | grep 'TX bytes'")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10);

        ExpectedOutcome shouldNeverResolve = new ExpectedOutcome()
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("Docker interface should not be here")
                .withExpectedOutput("docker")
                .withBinaryRelation(BinaryRelation.CONTAINS);

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(shouldNeverResolve);
        command.setExpectedOutcomes(expectedOutcomes);

        return command;
    }

    public static ICommand blockPartCommand(String blockID, String commandID){
        ICommand command = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("echo $var.block.id $var.command.id")
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(10)
                .withOutcomeAggregation(LogicalCondition.AND)
                .withAggregatedOutcomeMessage("Command params found in response");

        Map<String, String> dynamicFields = new HashMap<>();
        dynamicFields.put("\\$var.block.id", blockID);
        dynamicFields.put("\\$var.command.id", commandID);
        command.addDynamicFields(dynamicFields);

        ExpectedOutcome shouldMatchBlockId = new ExpectedOutcome()
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("Block ID has benn matched")
                .withExpectedOutput(blockID)
                .withBinaryRelation(BinaryRelation.CONTAINS);

        ExpectedOutcome shouldMatchCommandId = new ExpectedOutcome()
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("Command ID has benn matched")
                .withExpectedOutput(commandID)
                .withBinaryRelation(BinaryRelation.CONTAINS);

        List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
        expectedOutcomes.add(shouldMatchBlockId);
        expectedOutcomes.add(shouldMatchCommandId);
        command.setExpectedOutcomes(expectedOutcomes);

        return command;
    }

}
