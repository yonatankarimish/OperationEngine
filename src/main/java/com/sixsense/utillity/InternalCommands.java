package com.sixsense.utillity;

import com.sixsense.model.commands.Command;
import com.sixsense.model.commands.ICommand;
import com.sixsense.model.logic.*;
import com.sixsense.model.pipes.FirstLinePipe;
import com.sixsense.model.pipes.LastLinePipe;
import com.sixsense.model.pipes.WhitespacePipe;
import com.sixsense.model.retention.RetentionType;
import com.sixsense.model.retention.ResultRetention;

public class InternalCommands {
    public static ICommand invalidateCurrentPrompt(String channelType) {
        ICommand lastChunk = new Command()
            .withChannelName(channelType)
            .withCommandText("")
            .withMinimalSecondsToResponse(2)
            .withSecondsToTimeout(10)
            .withUseRawOutput(true)
            .addRetentionPipe(new FirstLinePipe())
            .addRetentionPipe(new WhitespacePipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue(MessageLiterals.LineBreak)
                    )
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
            .addDynamicField(DynamicFieldGlossary.var_scp_source, sourceFile)
            .addDynamicField(DynamicFieldGlossary.var_scp_source_file_name, splitSourceFileName[splitSourceFileName.length - 1])
            .addDynamicField(DynamicFieldGlossary.var_scp_destination, destFile);
    }
}