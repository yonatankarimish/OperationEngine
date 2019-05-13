package com.SixSense.util;

import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.data.pipes.LastLinePipe;

public class InternalCommands {
    public static ICommand invalidateCurrentPrompt(String channelType){
        ICommand lastChunk = new Command()
                .withChannelName(channelType)
                .withCommandText("")
                //.withMinimalSecondsToResponse(2)
                .withSecondsToTimeout(10)
                //.withUseRawOutput(true)
                .addOutputPipe(new LastLinePipe())
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("")
                                .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                                .withOutcome(ResultStatus.SUCCESS)
                )
                .withSaveTo(new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName("sixsense.vars.lastLine")
                );

        ICommand currentChunk = new Command()
                .withChannelName(channelType)
                .withCommandText("")
                //.withMinimalSecondsToResponse(2)
                .withSecondsToTimeout(10)
                //.withUseRawOutput(true)
                .addOutputPipe(new LastLinePipe())
                .addExpectedOutcome(new ExpectedOutcome()
                        .withExpectedValue("$sixsense.session.lastLine")
                        .withBinaryRelation(BinaryRelation.EQUALS)
                        .withOutcome(ResultStatus.SUCCESS)
                ).addExpectedOutcome(
                        new ExpectedOutcome()
                        .withExpectedValue("")
                        .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                        .withOutcome(ResultStatus.SUCCESS)
                ).withSaveTo(new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName("sixsense.session.prompt."+channelType.toLowerCase())
                );

        return lastChunk.chainCommands(currentChunk);
    }

    public static Command assignValue(String assignedField, String expression){
        return (Command)new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("expr " + expression)
                .withMinimalSecondsToResponse(1)
                .withSecondsToTimeout(5)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("")
                                .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                                .withOutcome(ResultStatus.SUCCESS)
                )
                .withSaveTo(new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName(assignedField)
                        .withOverwriteParent(true)
                );
    }

    public static ICommand copyFile(String sourceFile, String destFile, String host, String user, String password, int secondsToTimeout){
        ICommand scpInit = new Command()
                .withChannel(ChannelType.DOWNLOAD)
                .withCommandText("scp $var.scp.user@$var.scp.host:$var.scp.source $sixsense.session.workingDir/$var.scp.destination")
                .withSecondsToTimeout(10)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                        .withExpectedValue("assword:")
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withOutcome(ResultStatus.SUCCESS))
                ;

        ICommand typePassword = new Command()
                .withChannel(ChannelType.DOWNLOAD)
                .withCommandText("$var.scp.password")
                .withSecondsToTimeout(secondsToTimeout)
                .withUseRawOutput(true)
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("$var.scp.source_file_name.*\\n\\Q$sixsense.session.prompt.download\\E")
                                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                                .withOutcome(ResultStatus.SUCCESS)
                )
                .addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("[pP]assword:?\\s+[pP]assword:?")
                                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                                .withOutcome(ResultStatus.FAILURE)
                ).addExpectedOutcome(
                        new ExpectedOutcome()
                                .withExpectedValue("No such")
                                .withBinaryRelation(BinaryRelation.CONTAINS)
                                .withOutcome(ResultStatus.FAILURE)
                );

        String[] splitSourceFileName = sourceFile.split("/");
        return scpInit.chainCommands(typePassword)
                .addDynamicField("var.scp.host", host)
                .addDynamicField("var.scp.user", user)
                .addDynamicField("var.scp.source", sourceFile)
                .addDynamicField("var.scp.source_file_name", splitSourceFileName[splitSourceFileName.length - 1])
                .addDynamicField("var.scp.destination", destFile)
                .addDynamicField("var.scp.password", password);
    }
}
