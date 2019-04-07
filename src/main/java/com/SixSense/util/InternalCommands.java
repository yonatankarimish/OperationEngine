package com.SixSense.util;

import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.outcomes.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;

import java.util.ArrayList;
import java.util.List;

public class InternalCommands {
    public static ICommand invalidateCurrentPrompt(){
        ICommand lastChunk = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("")
                .withMinimalSecondsToResponse(2)
                .withSecondsToTimeout(100)
                .withUseRawOutput(true)
                .withSaveTo(new VariableRetention()
                    .withResultRetention(ResultRetention.Variable)
                    .withName("sixsense.vars.lastLine")
                );

        ExpectedOutcome lastLine = new ExpectedOutcome()
                .withExpectedValue("(?<=\n).+$")
                .withBinaryRelation(BinaryRelation.MATCHES_REGEX)
                .withOutcome(ResultStatus.SUCCESS);
        List<ExpectedOutcome> chunkOutcomes = new ArrayList<>();
        chunkOutcomes.add(lastLine);
        lastChunk.setExpectedOutcomes(chunkOutcomes);

        ICommand parseLastLine = new Command()
                .withCommandType(CommandType.LOCAL)
                .withCommandText("echo '$sixsense.vars.lastLine' | grep -v -e '^$' | tail -n 1")
                .withMinimalSecondsToResponse(2)
                .withSecondsToTimeout(100)
                .withSaveTo(new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName("sixsense.vars.lastLine")
                );

        ExpectedOutcome notEmpty = new ExpectedOutcome()
                .withExpectedValue("")
                .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                .withOutcome(ResultStatus.SUCCESS);
        List<ExpectedOutcome> lastLineOutcomes = new ArrayList<>();
        lastLineOutcomes.add(notEmpty);
        parseLastLine.setExpectedOutcomes(lastLineOutcomes);

        ICommand currentChunk = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("")
                .withMinimalSecondsToResponse(2)
                .withSecondsToTimeout(100)
                .withUseRawOutput(true)
                .withSaveTo(new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName("sixsense.vars.currentLine")
                );

        currentChunk.setExpectedOutcomes(chunkOutcomes);

        ICommand parseCurrentLine = new Command()
                .withCommandType(CommandType.LOCAL)
                .withCommandText("echo '$sixsense.vars.currentLine' | grep -v -e '^$' | tail -n 1")
                .withMinimalSecondsToResponse(2)
                .withSecondsToTimeout(100)
                .withOutcomeAggregation(LogicalCondition.AND)
                .withSaveTo(new VariableRetention()
                        .withResultRetention(ResultRetention.Variable)
                        .withName("sixsense.vars.remotePrompt")
                );

        ExpectedOutcome linesEqual = new ExpectedOutcome()
                .withExpectedValue("$sixsense.vars.lastLine")
                .withBinaryRelation(BinaryRelation.EQUALS)
                .withOutcome(ResultStatus.SUCCESS);
        List<ExpectedOutcome> comparisonOutcomes = new ArrayList<>();
        comparisonOutcomes.add(linesEqual);
        comparisonOutcomes.add(notEmpty);
        parseCurrentLine.setExpectedOutcomes(comparisonOutcomes);

        return lastChunk
                .chainCommands(parseLastLine)
                .chainCommands(currentChunk)
                .chainCommands(parseCurrentLine);
    }
}
