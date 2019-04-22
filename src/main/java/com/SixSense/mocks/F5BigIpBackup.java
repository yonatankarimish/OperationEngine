package com.SixSense.mocks;

import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.devices.Device;
import com.SixSense.data.devices.VendorProductVersion;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.util.InternalCommands;

public class F5BigIpBackup {
    public static Operation f5BigIpBackup(String host, String username, String password){
        ICommand operationBlock = sshConnect()
                .chainCommands(rebuildSixSenseDirectory())
                .chainCommands(etcHosts())
                .chainCommands(exitCommand())
                .addDynamicField("device.host", host)
                .addDynamicField("device.username", username)
                .addDynamicField("device.password", password)
                .addDynamicField("device.port", "22");

        return new Operation()
                .withDevice(new Device().withVpv(
                        new VendorProductVersion()
                        .withVendor("F5")
                        .withProduct("BigIP")
                        .withVersion("11 and above")
                ))
                .withOperationName("Configuration Backup")
                .withExecutionBlock(operationBlock)
                .addChannel(ChannelType.LOCAL)
                .addChannel(ChannelType.REMOTE)
                .addChannel(ChannelType.DOWNLOAD);
    }

    private static ICommand sshConnect(){
        ExpectedOutcome template = new ExpectedOutcome()
                .withBinaryRelation(BinaryRelation.CONTAINS)
                .withOutcome(ResultStatus.SUCCESS);

        ICommand ssh = new Command()
                .withChannel(ChannelType.REMOTE)
                .withCommandText("ssh $device.username@$device.host -p $device.port")
                //.withMinimalSecondsToResponse(5)
                .withSecondsToTimeout(60)
                .addExpectedOutcome(template.deepClone().withExpectedValue("assword:"))
                .addExpectedOutcome(template.deepClone().withExpectedValue("connecting (yes/no)"))
                .addExpectedOutcome(template.deepClone().withExpectedValue("connecting"))
                .addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("REMOTE HOST IDENTIFICATION HAS CHANGE")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("SSH Key Mismatch")
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("Connection timed out")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection timed out")
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("Connection refused")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection refused")
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("No route to host")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("No route to host")
                ).withSaveTo(new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName("ssh.connect.response")
                );

        ICommand rsaFirstTime = new Command()
                .withChannel(ChannelType.REMOTE)
                .withCommandText("yes")
                //.withMinimalSecondsToResponse(5)
                .withSecondsToTimeout(60)
                .addExecutionCondition(
                        new ExecutionCondition()
                        .withVariable("$ssh.connect.response")
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("connecting (yes/no)")
                ).addExpectedOutcome(template.deepClone().withExpectedValue("assword:"))
                .addExpectedOutcome(template.deepClone().withExpectedValue("denied"))
                .addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("Name or service not known")
                        .withOutcome(ResultStatus.FAILURE)
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("REMOTE HOST IDENTIFICATION HAS CHANGE")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("SSH Key Mismatch")
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("Connection timed out")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection timed out")
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("Connection refused")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection refused")
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("No route to host")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("No route to host")
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("Invalid argument")
                        .withOutcome(ResultStatus.FAILURE)
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
                .addExecutionCondition(
                        new ExecutionCondition()
                        .withVariable("$device.password")
                        .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                        .withExpectedValue("")
                ).addExpectedOutcome(template.deepClone().withExpectedValue("#"))
                .addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("onnection")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Connection refused")
                ).addExpectedOutcome(
                        template.deepClone()
                        .withExpectedValue("enied")
                        .withOutcome(ResultStatus.FAILURE)
                        .withMessage("Wrong username or password")
                );

        ICommand tmsh = new Command()
                .withChannel(ChannelType.REMOTE)
                .withCommandText("tmsh modify cli preference pager disabled")
                //.withMinimalSecondsToResponse(5)
                .withSecondsToTimeout(30)
                .withUseRawOutput(true)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                        .withExpectedValue("tmsh.*\\n\\Q$sixsense.session.prompt.remote\\E")
                        .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                        .withOutcome(ResultStatus.SUCCESS)
                );

        return ssh.chainCommands(rsaFirstTime)
                .chainCommands(typePassword)
                .chainCommands(InternalCommands.invalidateCurrentPrompt(ChannelType.REMOTE.name()))
                .chainCommands(tmsh);
    }

    private static ICommand rebuildSixSenseDirectory(){
        ICommand deleteOldDir = new Command()
                .withChannel(ChannelType.REMOTE)
                .withCommandText("rm -rf /var/SixSense")
                .withSecondsToTimeout(600)
                .withUseRawOutput(true)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("rm -rf.*\\n\\Q$sixsense.session.prompt.remote\\E")
                                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                                .withOutcome(ResultStatus.SUCCESS)
                );

        ICommand makeNewDir = new Command()
                .withChannel(ChannelType.REMOTE)
                .withCommandText("mkdir -p /var/SixSense")
                .withSecondsToTimeout(15)
                .withUseRawOutput(true)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("mkdir -p.*\\n\\Q$sixsense.session.prompt.remote\\E")
                                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                                .withOutcome(ResultStatus.SUCCESS)
                );

        return deleteOldDir.chainCommands(makeNewDir);
    }

    private static ICommand etcHosts(){
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
    }

    private static ICommand exitCommand(){
        return new Command()
                .withChannel(ChannelType.REMOTE)
                .withCommandText("exit")
                .withSecondsToTimeout(10);
    }
}
