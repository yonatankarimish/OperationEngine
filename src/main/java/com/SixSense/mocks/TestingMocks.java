package com.SixSense.mocks;

import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.devices.Credentials;
import com.SixSense.data.devices.Device;
import com.SixSense.data.devices.RawExecutionConfig;
import com.SixSense.data.devices.VendorProductVersion;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.util.FileUtils;
import com.SixSense.util.InternalCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class TestingMocks {
    private static final Logger logger = LogManager.getLogger(TestingMocks.class);

    public static void testCompressionAndDecompression(){
        String filename = "";
        try {
            filename = "/tmp/one_megabyte.txt";
            FileUtils.compress(filename);
            logger.info("File " + filename + " compressed successfully");
        }catch (Exception e){
            logger.error("Failed to compress file "+filename, e);
        }

        try {
            filename = "/tmp/one_megabyte.txt";
            String filenameCompressed = filename+".lz4";
            FileUtils.decompress(filenameCompressed, "/tmp/one_megabyte_restored.txt");
            logger.info("File " + filename + " compressed successfully");
        }catch (Exception e){
            logger.error("Failed to decompress file "+filename, e);
        }
    }

    public static RawExecutionConfig f5BigIpBackup(List<Credentials> credentialList){
        VendorProductVersion f5BigIp = new VendorProductVersion()
            .withVendor("F5")
            .withProduct("BigIP")
            .withVersion("11 and above");

        return new RawExecutionConfig()
            .withOperation(
                new Operation()
                    .withOperationName("Configuration Backup")
                    .withExecutionBlock(
                        new Block()
                            .addChildBlock(sshConnect())
                            .addChildBlock(rebuildSixSenseDirectory())
                            .addChildBlock(etcHosts())
                            .addChildBlock(exitCommand())
                    )
                    .addChannel(ChannelType.LOCAL)
                    .addChannel(ChannelType.REMOTE)
                    .addChannel(ChannelType.DOWNLOAD)
            )
            .withDevices(
                credentialList.stream()
                    .map(
                        credentials -> new Device()
                            .withCredentials(credentials)
                            .withVpv(f5BigIp.deepClone())
                    )
                    .collect(Collectors.toList())
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

    private static ICommand rebuildSixSenseDirectory(){
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
                        new VariableRetention()
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

    private static ICommand exitCommand(){
        return new Command()
            .withChannel(ChannelType.REMOTE)
            .withCommandText("exit")
            .withSecondsToTimeout(10);
    }
}
