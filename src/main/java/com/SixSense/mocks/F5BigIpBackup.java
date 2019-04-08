package com.SixSense.mocks;

import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.outcomes.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.util.InternalCommands;

public class F5BigIpBackup {
    public static Operation f5BigIpBackup(String host, String username, String password){
        ICommand operationBlock = sshConnect()
                .chainCommands(rebuildSixSenseDirectory())
                .chainCommands(exitCommand())
                .addDynamicField("device.host", host)
                .addDynamicField("device.username", username)
                .addDynamicField("device.password", password)
                .addDynamicField("device.port", "22");

        return new Operation()
                .withVPV("F5 BigIP Version 11 and above")
                .withOperationName("Configuration Backup")
                .withExecutionBlock(operationBlock);
    }

    private static ICommand sshConnect(){
        ExpectedOutcome template = new ExpectedOutcome()
                .withBinaryRelation(BinaryRelation.CONTAINS)
                .withOutcome(ResultStatus.SUCCESS);

        ICommand ssh = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ssh $device.username@$device.host -p $device.port")
                //.withMinimalSecondsToResponse(5)
                .withSecondsToTimeout(60)
                .addExpectedOutcome(new ExpectedOutcome(template).withExpectedValue("assword:"))
                .addExpectedOutcome(new ExpectedOutcome(template).withExpectedValue("connecting (yes/no)"))
                .addExpectedOutcome(new ExpectedOutcome(template).withExpectedValue("connecting"))
                .addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("REMOTE HOST IDENTIFICATION HAS CHANGE")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("SSH Key Mismatch")
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("Connection timed out")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection timed out")
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("Connection refused")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection refused")
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("No route to host")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("No route to host")
                ).withSaveTo(new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName("ssh.connect.response")
                );

        ICommand rsaFirstTime = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("yes")
                //.withMinimalSecondsToResponse(5)
                .withSecondsToTimeout(60)
                .addExecutionCondition(
                        new ExecutionCondition()
                        .withVariable("ssh.connect.response")
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("connecting (yes/no)")
                ).addExpectedOutcome(new ExpectedOutcome(template).withExpectedValue("assword:"))
                .addExpectedOutcome(new ExpectedOutcome(template).withExpectedValue("denied"))
                .addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("Name or service not known")
                        .withOutcome(ResultStatus.FAILURE)
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("REMOTE HOST IDENTIFICATION HAS CHANGE")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("SSH Key Mismatch")
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("Connection timed out")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection timed out")
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("Connection refused")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection refused")
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("No route to host")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("No route to host")
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("Invalid argument")
                        .withOutcome(ResultStatus.FAILURE)
                ).withSaveTo(
                        new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName("ssh.connect.response")
                );

        ICommand typePassword = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("$device.password")
                //.withMinimalSecondsToResponse(5)
                .withSecondsToTimeout(60)
                .addExecutionCondition(
                        new ExecutionCondition()
                        .withVariable("device.password")
                        .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                        .withExpectedValue("")
                ).addExpectedOutcome(new ExpectedOutcome(template).withExpectedValue("#"))
                .addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("onnection")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection refused")
                ).addExpectedOutcome(
                        new ExpectedOutcome(template)
                        .withExpectedValue("enied")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Wrong username or password")
                );

        ICommand tmsh = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("tmsh modify cli preference pager disabled")
                //.withMinimalSecondsToResponse(5)
                .withSecondsToTimeout(30)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                        .withExpectedValue("")
                        .withBinaryRelation(BinaryRelation.EQUALS)
                        .withOutcome(ResultStatus.SUCCESS)
                );

        return ssh.chainCommands(rsaFirstTime)
                .chainCommands(typePassword)
                .chainCommands(InternalCommands.invalidateCurrentPrompt())
                .chainCommands(tmsh);
    }

    private static ICommand rebuildSixSenseDirectory(){
        ICommand deleteOldDir = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("rm -rf /var/SixSense")
                .withSecondsToTimeout(600)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                        .withExpectedValue("")
                        .withBinaryRelation(BinaryRelation.EQUALS)
                        .withOutcome(ResultStatus.SUCCESS)
                );

        ICommand makeNewDir = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("mkdir -p /var/SixSense")
                .withSecondsToTimeout(15)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                        .withExpectedValue("")
                        .withBinaryRelation(BinaryRelation.EQUALS)
                        .withOutcome(ResultStatus.SUCCESS)
                );

        return deleteOldDir.chainCommands(makeNewDir);
    }

    private static ICommand exitCommand(){
        return new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("exit")
                .withSecondsToTimeout(10);
    }
}
