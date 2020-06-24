package com.sixsense.mocks;

import com.sixsense.model.commands.Block;
import com.sixsense.model.commands.Command;
import com.sixsense.model.commands.ICommand;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.devices.Credentials;
import com.sixsense.model.devices.Device;
import com.sixsense.model.wrappers.AdministrativeConfig;
import com.sixsense.model.wrappers.RawExecutionConfig;
import com.sixsense.model.devices.VendorProductVersion;
import com.sixsense.model.logic.*;
import com.sixsense.model.pipes.ClearingPipe;
import com.sixsense.model.pipes.FirstLinePipe;
import com.sixsense.model.pipes.LastLinePipe;
import com.sixsense.model.pipes.WhitespacePipe;
import com.sixsense.model.retention.RetentionMode;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.utillity.InternalCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class OperationMocks {
    private static final Logger logger = LogManager.getLogger(OperationMocks.class);

    public static RawExecutionConfig f5BigIpBackup(List<Credentials> credentialList){
        VendorProductVersion f5BigIp = new VendorProductVersion()
            .withVendor("F5")
            .withProduct("BigIP")
            .withVersion("11 and above");

        return new RawExecutionConfig()
            .withAdministrativeConfig(
                new AdministrativeConfig()
                    .addDevices(
                        credentialList.stream()
                            .map(
                                credentials -> new Device()
                                    .withCredentials(credentials)
                                    .withVpv(f5BigIp.deepClone())
                            )
                            .collect(Collectors.toList())
                    )
            )
            .withOperation(
                new Operation()
                    .withOperationName("Configuration Backup")
                    .withExecutionBlock(
                        new Block()
                            .addChildBlock(sshConnect())
                            .addChildBlock(rebuildSixSenseDirectory())
                            .addChildBlock(etcHosts())
                            .addChildBlock(inventory())
                            .addChildBlock(exitCommand())
                    )
                    .addChannel(ChannelType.LOCAL)
                    .addChannel(ChannelType.REMOTE)
                    .addChannel(ChannelType.DOWNLOAD)
            );
    }

    public static RawExecutionConfig f5BigIpInventory(List<Credentials> credentialList){
        VendorProductVersion f5BigIp = new VendorProductVersion()
            .withVendor("F5")
            .withProduct("BigIP")
            .withVersion("11 and above");

        return new RawExecutionConfig()
            .withAdministrativeConfig(
                new AdministrativeConfig()
                    .addDevices(
                        credentialList.stream()
                            .map(
                                credentials -> new Device()
                                    .withCredentials(credentials)
                                    .withVpv(f5BigIp.deepClone())
                            )
                            .collect(Collectors.toList())
                    )
            )
            .withOperation(
                new Operation()
                    .withOperationName("Configuration Backup")
                    .withExecutionBlock(
                        new Block()
                            .addChildBlock(sshConnect())
                            .addChildBlock(inventory())
                            .addChildBlock(exitCommand())
                    )
                    .addChannel(ChannelType.LOCAL)
                    .addChannel(ChannelType.REMOTE)
            );
    }

    private static ICommand sshConnect(){
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
                    .withExpectedValue("connecting (yes/no")
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
            ).withSaveTo(new ResultRetention()
                .withRetentionMode(RetentionMode.Variable)
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
                        .withExpectedValue("connecting (yes/no")
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
                new ResultRetention()
                    .withRetentionMode(RetentionMode.Variable)
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
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("tmsh")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("$sixsense.session.prompt.remote")
                    )
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
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("rm -rf")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("$sixsense.session.prompt.remote")
                    )
            );

        ICommand makeNewDir = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("mkdir -p /var/SixSense")
            .withSecondsToTimeout(15)
            .withUseRawOutput(true)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("mkdir -p")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("$sixsense.session.prompt.remote")
                    )
            );

        return deleteOldDir.chainCommands(makeNewDir);
    }

    //get file by using 'cat' command and saving to file
    /*private static ICommand etcHosts(){
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
                        new ResultRetention()
                        .withResultRetention(ResultRetention.File)
                        .withName("hosts.txt")
                );
    }*/

    //get file by running scp command using dedicated download channel
    private static ICommand etcHosts(){
        return InternalCommands.copyFile(
            "/etc/hosts",
            "hosts.txt",
            45
        );
    }

    private static ICommand inventory(){
        ICommand memory = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("cat /proc/meminfo | awk 'NR < 2 {print $2,$3}'")
            .withSecondsToTimeout(15)
            .addOutputPipe(new LastLinePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.ENDS_WITH)
                        .withExpectedValue("kB")
                )
            ).withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.DatabaseEventual)
                    .withName("var.inventory.memory")
            );

        ICommand cpu = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("cat /proc/cpuinfo | grep 'model name' | awk 'NR < 2' | sed 's/model name/ /g' |sed 's/://' | sed 's/^[[:space:]]*//' | sed 's/  */\\ /g'")
            .withSecondsToTimeout(15)
            .addOutputPipe(new LastLinePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("GHz")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("MHz")
                    )
            ).withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.DatabaseEventual)
                    .withName("var.inventory.cpu")
            );

        ICommand freeSpaceRoot = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("df -lh | grep /$ | awk '{print $4}'")
            .withSecondsToTimeout(15)
            .addOutputPipe(new LastLinePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addExpression(
                        new LogicalExpression<ExpectedOutcome>()
                            .withLogicalCondition(LogicalCondition.AND)
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.NOT_CONTAINS)
                                    .withExpectedValue("GHz")
                            )
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.NOT_CONTAINS)
                                    .withExpectedValue("MHz")
                            )
                    )
                    .addExpression(
                        new LogicalExpression<ExpectedOutcome>()
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.ENDS_WITH)
                                    .withExpectedValue("K")
                            )
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.ENDS_WITH)
                                    .withExpectedValue("M")
                            )
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.ENDS_WITH)
                                    .withExpectedValue("G")
                            )
                    )
            ).withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.DatabaseEventual)
                    .withName("var.inventory.space.root")
            );

        ICommand freeSpaceVar = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("df -lh | grep /var$ | awk '{print $4}'")
            .withSecondsToTimeout(15)
            .addOutputPipe(new LastLinePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addExpression(
                        new LogicalExpression<ExpectedOutcome>()
                            .withLogicalCondition(LogicalCondition.AND)
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.NOT_CONTAINS)
                                    .withExpectedValue("GHz")
                            )
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.NOT_CONTAINS)
                                    .withExpectedValue("MHz")
                            )
                    )
                    .addExpression(
                        new LogicalExpression<ExpectedOutcome>()
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.ENDS_WITH)
                                    .withExpectedValue("K")
                            )
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.ENDS_WITH)
                                    .withExpectedValue("M")
                            )
                            .addResolvable(
                                new ExpectedOutcome()
                                    .withBinaryRelation(BinaryRelation.ENDS_WITH)
                                    .withExpectedValue("G")
                            )
                    )
            ).withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.DatabaseEventual)
                    .withName("var.inventory.space.var")
            );

        ICommand uptime = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("uptime | awk '{print $3\" \"$4}' | sed 's|,||g'")
            .withSecondsToTimeout(15)
            .addOutputPipe(new LastLinePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("day")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("hour")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("min")
                    )
            ).withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.DatabaseEventual)
                    .withName("var.inventory.uptime")
            );

        ICommand tmsh = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("tmsh")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                        .withExpectedValue("")
                )
            );

        ICommand chassisTxt = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("show sys hardware | grep 'Chassis Serial'")
            .withSecondsToTimeout(15)
            .withUseRawOutput(true)
            .addRetentionPipe(new ClearingPipe())
            .addRetentionPipe(new LastLinePipe())
            .addRetentionPipe(new WhitespacePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("Chassis Serial")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("$sixsense.session.prompt.remote")
                    )
            ).withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.Variable)
                    .withName("var.parsing.chassis")
            );

        ICommand chassisParse = new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo $var.parsing.chassis | awk '{print $3}'")
            .withSecondsToTimeout(15)
            .withUseRawOutput(true)
            .addRetentionPipe(new ClearingPipe())
            .addRetentionPipe(new LastLinePipe())
            .addRetentionPipe(new FirstLinePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("awk '{print $3}'")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("$sixsense.session.prompt.local")
                    )
            ).withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.DatabaseEventual)
                    .withName("var.inventory.chassis")
            );

        ICommand quit = new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("quit")
            .withSecondsToTimeout(10);


        return memory.chainCommands(cpu)
            .chainCommands(freeSpaceRoot)
            .chainCommands(freeSpaceVar)
            .chainCommands(uptime)
            .chainCommands(tmsh)
            .chainCommands(InternalCommands.invalidateCurrentPrompt(ChannelType.REMOTE.name()))
            .chainCommands(chassisTxt)
            .chainCommands(chassisParse)
            .chainCommands(quit);
    }

    private static ICommand exitCommand(){
        return new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("exit")
            .withSecondsToTimeout(10);
    }
}
