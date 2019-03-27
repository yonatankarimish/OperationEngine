package com.SixSense.mocks;

import com.SixSense.data.Outcomes.BinaryRelation;
import com.SixSense.data.Outcomes.CommandType;
import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.data.Outcomes.ResultStatus;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Operation;

import java.util.ArrayList;
import java.util.List;

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
                .withOperationName("Network Details")
                .withExecutionBlock(localBlock);
    }

    public static ICommand dockerInterface(){
        Command command = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ifconfig | grep 'docker';")
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
                .withCommandText("ifconfig | grep 'inet addr' | head -n 2 | tail -1;")
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
                .withCommandText("ifconfig | grep 'inet addr' | head -n 2 | tail -1;")
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

}
