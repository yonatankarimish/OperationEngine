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

    public static String rawExecutionConfigJsonString(){
        return "{\"devices\":[{\"credentials\":{\"host\":\"172.31.252.179\",\"password\":\"qwe123\",\"port\":22,\"username\":\"root\"},\"dynamicFields\":{},\"vpv\":{\"product\":\"BigIP\",\"vendor\":\"F5\",\"version\":\"11 and above\"}}],\"operation\":{\"@class\":\"Operation\",\"alreadyExecuted\":false,\"channelNames\":[\"LOCAL\",\"DOWNLOAD\",\"REMOTE\"],\"dynamicFields\":{},\"executionBlock\":{\"@class\":\"Block\",\"alreadyExecuted\":false,\"childBlocks\":[{\"@class\":\"Block\",\"alreadyExecuted\":false,\"childBlocks\":[{\"@class\":\"Command\",\"alreadyExecuted\":false,\"channelName\":\"REMOTE\",\"commandText\":\"ssh $device.username@$device.host -p $device.port\",\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"assword:\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"connecting (yes/no)\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"connecting\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"REMOTE HOST IDENTIFICATION HAS CHANGE\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"SSH Key Mismatch\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"Connection timed out\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Connection timed out\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"Connection refused\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Connection refused\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"No route to host\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"No route to host\"}}]},\"minimalSecondsToResponse\":0,\"outputPipes\":[],\"saveTo\":{\"name\":\"ssh.connect.response\",\"overwriteParent\":false,\"resultRetention\":\"Variable\",\"value\":\"\"},\"secondsToTimeout\":60,\"useRawOutput\":false},{\"@class\":\"Command\",\"alreadyExecuted\":false,\"channelName\":\"REMOTE\",\"commandText\":\"yes\",\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[{\"@class\":\"ExecutionCondition\",\"variable\":\"$ssh.connect.response\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"connecting (yes/no)\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}}]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"assword:\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"denied\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"Name or service not known\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Name or service not known\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"REMOTE HOST IDENTIFICATION HAS CHANGE\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"SSH Key Mismatch\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"Connection timed out\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Connection timed out\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"Connection refused\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Connection refused\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"No route to host\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"No route to host\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"Invalid argument\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Invalid argument\"}}]},\"minimalSecondsToResponse\":0,\"outputPipes\":[],\"saveTo\":{\"name\":\"ssh.connect.response\",\"overwriteParent\":false,\"resultRetention\":\"Variable\",\"value\":\"\"},\"secondsToTimeout\":60,\"useRawOutput\":false},{\"@class\":\"Command\",\"alreadyExecuted\":false,\"executionCondition\":{\"@class\":\"LogicalExpression\",\"resolvableExpressions\":[{\"@class\":\"ExecutionCondition\",\"variable\":\"$device.password\",\"binaryRelation\":\"NOT_EQUALS\",\"expectedValue\":\"\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}}],\"logicalCondition\":\"OR\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"#\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"onnection\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Connection refused\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"enied\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Wrong username or password\"}}],\"logicalCondition\":\"OR\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},\"dynamicFields\":{},\"saveTo\":{\"resultRetention\":\"None\",\"name\":\"\",\"value\":\"\",\"overwriteParent\":false},\"channelName\":\"REMOTE\",\"commandText\":\"$device.password\",\"minimalSecondsToResponse\":0,\"secondsToTimeout\":60,\"useRawOutput\":false,\"outputPipes\":[]},{\"@class\":\"Command\",\"alreadyExecuted\":false,\"executionCondition\":{\"@class\":\"LogicalExpression\",\"resolvableExpressions\":[],\"logicalCondition\":\"OR\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"NOT_EQUALS\",\"expectedValue\":\"\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}}],\"logicalCondition\":\"OR\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},\"dynamicFields\":{},\"saveTo\":{\"resultRetention\":\"Variable\",\"name\":\"sixsense.vars.lastLine\",\"value\":\"\",\"overwriteParent\":false},\"channelName\":\"REMOTE\",\"commandText\":\"\",\"minimalSecondsToResponse\":0,\"secondsToTimeout\":10,\"useRawOutput\":false,\"outputPipes\":[{\"@class\":\"LastLinePipe\"}]},{\"@class\":\"Command\",\"alreadyExecuted\":false,\"executionCondition\":{\"@class\":\"LogicalExpression\",\"resolvableExpressions\":[],\"logicalCondition\":\"OR\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"EQUALS\",\"expectedValue\":\"$sixsense.session.lastLine\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"NOT_EQUALS\",\"expectedValue\":\"\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}}],\"logicalCondition\":\"OR\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},\"dynamicFields\":{},\"saveTo\":{\"resultRetention\":\"Variable\",\"name\":\"sixsense.session.prompt.remote\",\"value\":\"\",\"overwriteParent\":false},\"channelName\":\"REMOTE\",\"commandText\":\"\",\"minimalSecondsToResponse\":0,\"secondsToTimeout\":10,\"useRawOutput\":false,\"outputPipes\":[{\"@class\":\"LastLinePipe\"}]},{\"@class\":\"Command\",\"alreadyExecuted\":false,\"executionCondition\":{\"@class\":\"LogicalExpression\",\"resolvableExpressions\":[],\"logicalCondition\":\"OR\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"MATCHES_REGEX\",\"expectedValue\":\"tmsh.*\\\\n\\\\Q$sixsense.session.prompt.remote\\\\E\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}}],\"logicalCondition\":\"OR\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"SUCCESS\",\"message\":\"\"}},\"dynamicFields\":{},\"saveTo\":{\"resultRetention\":\"None\",\"name\":\"\",\"value\":\"\",\"overwriteParent\":false},\"channelName\":\"REMOTE\",\"commandText\":\"tmsh modify cli preference pager disabled\",\"minimalSecondsToResponse\":0,\"secondsToTimeout\":30,\"useRawOutput\":true,\"outputPipes\":[]}],\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"repeatCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"}},{\"@class\":\"Block\",\"alreadyExecuted\":false,\"childBlocks\":[{\"@class\":\"Command\",\"alreadyExecuted\":false,\"channelName\":\"REMOTE\",\"commandText\":\"rm -rf /var/SixSense\",\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"MATCHES_REGEX\",\"expectedValue\":\"rm -rf.*\\\\n\\\\Q$sixsense.session.prompt.remote\\\\E\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false}}]},\"minimalSecondsToResponse\":0,\"outputPipes\":[],\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"},\"secondsToTimeout\":600,\"useRawOutput\":true},{\"@class\":\"Command\",\"alreadyExecuted\":false,\"channelName\":\"REMOTE\",\"commandText\":\"mkdir -p /var/SixSense\",\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"MATCHES_REGEX\",\"expectedValue\":\"mkdir -p.*\\\\n\\\\Q$sixsense.session.prompt.remote\\\\E\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false}}]},\"minimalSecondsToResponse\":0,\"outputPipes\":[],\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"},\"secondsToTimeout\":15,\"useRawOutput\":true}],\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"repeatCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"}},{\"@class\":\"Block\",\"alreadyExecuted\":false,\"childBlocks\":[{\"@class\":\"Command\",\"alreadyExecuted\":false,\"channelName\":\"DOWNLOAD\",\"commandText\":\"scp $device.username@$device.host:$var.scp.source $sixsense.session.workingDir/$var.scp.destination\",\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"assword:\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false}}]},\"minimalSecondsToResponse\":0,\"outputPipes\":[],\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"},\"secondsToTimeout\":10,\"useRawOutput\":false},{\"@class\":\"Command\",\"alreadyExecuted\":false,\"channelName\":\"DOWNLOAD\",\"commandText\":\"$device.password\",\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"MATCHES_REGEX\",\"expectedValue\":\"$var.scp.source_file_name.*\\\\n\\\\Q$sixsense.session.prompt.download\\\\E\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"MATCHES_REGEX\",\"expectedValue\":\"[pP]assword:?\\\\s+[pP]assword:?\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"Wrong username or password\"}},{\"@class\":\"ExpectedOutcome\",\"binaryRelation\":\"CONTAINS\",\"expectedValue\":\"No such\",\"expressionResult\":{\"resolved\":false,\"outcome\":\"FAILURE\",\"message\":\"File does not exist\"}}]},\"minimalSecondsToResponse\":0,\"outputPipes\":[],\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"},\"secondsToTimeout\":45,\"useRawOutput\":true}],\"dynamicFields\":{\"var.scp.source\":\"/etc/hosts\",\"var.scp.source_file_name\":\"hosts\",\"var.scp.destination\":\"hosts.txt\"},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"repeatCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"}},{\"@class\":\"Command\",\"alreadyExecuted\":false,\"channelName\":\"REMOTE\",\"commandText\":\"exit\",\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"minimalSecondsToResponse\":0,\"outputPipes\":[],\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"},\"secondsToTimeout\":10,\"useRawOutput\":false}],\"dynamicFields\":{},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"repeatCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"}},\"executionCondition\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"expectedOutcome\":{\"@class\":\"LogicalExpression\",\"expressionResult\":{\"message\":\"\",\"outcome\":\"SUCCESS\",\"resolved\":false},\"logicalCondition\":\"OR\",\"resolvableExpressions\":[]},\"operationName\":\"Configuration Backup\",\"saveTo\":{\"name\":\"\",\"overwriteParent\":false,\"resultRetention\":\"None\",\"value\":\"\"},\"sequenceExecutionStarted\":false,\"sequentialWorkflowUponFailure\":[],\"sequentialWorkflowUponSuccess\":[]}}";
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
