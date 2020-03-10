package com.SixSense.util;

import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.logic.*;
import com.SixSense.data.pipes.FirstLinePipe;
import com.SixSense.data.pipes.LastLinePipe;
import com.SixSense.data.retention.RetentionType;
import com.SixSense.data.retention.ResultRetention;

public class InternalCommands {
    public static ICommand invalidateCurrentPrompt(String channelType) {
        ICommand lastChunk = new Command()
            .withChannelName(channelType)
            .withCommandText("")
            .withMinimalSecondsToResponse(2)
            .withSecondsToTimeout(10)
            .withUseRawOutput(true)
            .addOutputPipe(new LastLinePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    /*.withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue(MessageLiterals.LineBreak)
                    )*/
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                            .withExpectedValue("")
                    )
            )
            .withSaveTo(new ResultRetention()
                .withRetentionType(RetentionType.Variable)
                .withName("sixsense.session.lastLine")
            );

        ICommand currentChunk = new Command()
            .withChannelName(channelType)
            .withCommandText("")
            //.withMinimalSecondsToResponse(2)
            .withSecondsToTimeout(10)
            //.withUseRawOutput(true)
            .addOutputPipe(new LastLinePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.EQUALS)
                            .withExpectedValue("$sixsense.session.lastLine")
                    )
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                        .withExpectedValue("")
                    )
            )
            .withSaveTo(new ResultRetention()
                .withRetentionType(RetentionType.Variable)
                .withName("sixsense.session.prompt." + channelType.toLowerCase())
            );

        return lastChunk.chainCommands(currentChunk);
    }

    public static Command assignValue(String assignedField, String expression) {
        return (Command) new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("expr " + expression)
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(5)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                        .withExpectedValue("")
                )
            )
            .withSaveTo(new ResultRetention()
                .withRetentionType(RetentionType.Variable)
                .withName(assignedField)
                .withOverwriteParent(true)
            );
    }

    //Note this internal command depends on the implied presence of the credential dynamic fields for the relevant device
    public static ICommand copyFile(String sourceFile, String destFile, int secondsToTimeout) {
        ICommand scpInit = new Command()
            .withChannel(ChannelType.DOWNLOAD)
            .withCommandText("scp $device.username@$device.host:$var.scp.source $sixsense.session.workingDir/$var.scp.destination")
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("assword:")
                )
            );

        ICommand typePassword = new Command()
            .withChannel(ChannelType.DOWNLOAD)
            .withCommandText("$device.password")
            .withSecondsToTimeout(secondsToTimeout)
            .withUseRawOutput(true)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("$sixsense.session.prompt.download")
                    )
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                        .withExpectedValue("[pP]assword:?\\s+[pP]assword:?")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("Wrong username or password")
                        )
                    )
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("No such")
                        .withExpressionResult(
                            new ExpressionResult()
                                .withOutcome(ResultStatus.FAILURE)
                                .withMessage("File does not exist")
                        )
                    )
            );

        String[] splitSourceFileName = sourceFile.split("/");
        return scpInit.chainCommands(typePassword)
            .addDynamicField("var.scp.source", sourceFile)
            .addDynamicField("var.scp.source_file_name", splitSourceFileName[splitSourceFileName.length - 1])
            .addDynamicField("var.scp.destination", destFile);
    }
}
