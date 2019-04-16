package com.SixSense.util;

import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.data.pipes.LastLinePipe;

public class InternalCommands {
    public static ICommand invalidateCurrentPrompt(){
        ICommand lastChunk = new Command()
                .withCommandType(CommandType.REMOTE)
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
                .withCommandType(CommandType.REMOTE)
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
                        .withName("sixsense.session.remotePrompt")
                );

        return lastChunk.chainCommands(currentChunk);
    }

    public static Command assignValue(String assignedField, String expression){
        return (Command)new Command()
                .withCommandType(CommandType.LOCAL)
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
}
