package com.SixSense.util;

import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.data.Outcomes.LogicalCondition;

import java.util.List;

import static com.SixSense.util.MessageLiterals.CommandDidNotReachOutcome;

public class ExpectedOutcomeResolver {
    public static ExpectedOutcome resolveExpectedOutcome(List<String> commandOutout, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation){
        switch (outcomeAggregation) {
            case OR:{
                for (ExpectedOutcome possibleOutcome : expectedOutcomes) {
                    ExpectedOutcome resolvedOutcome = ExpectedOutcomeResolver.resolveExpectedOutcome(commandOutout, possibleOutcome);
                    if (resolvedOutcome.isResolved()) {
                        return resolvedOutcome;
                    }
                }
                return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
            }
            case NOR:{
                for (ExpectedOutcome possibleOutcome : expectedOutcomes) {
                    ExpectedOutcome resolvedOutcome = ExpectedOutcomeResolver.resolveExpectedOutcome(commandOutout, possibleOutcome);
                    if (resolvedOutcome.isResolved()) {
                        return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
                    }
                }
                return ExpectedOutcome.defaultOutcome();
            }
            case AND:{
                for (ExpectedOutcome possibleOutcome : expectedOutcomes) {
                    ExpectedOutcome resolvedOutcome = ExpectedOutcomeResolver.resolveExpectedOutcome(commandOutout, possibleOutcome);
                    if (!resolvedOutcome.isResolved()) {
                        return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
                    }
                }
                return ExpectedOutcome.defaultOutcome();
            }
            case NAND:{
                for (ExpectedOutcome possibleOutcome : expectedOutcomes) {
                    ExpectedOutcome resolvedOutcome = ExpectedOutcomeResolver.resolveExpectedOutcome(commandOutout, possibleOutcome);
                    if (!resolvedOutcome.isResolved()) {
                        return resolvedOutcome.withOutcome(resolvedOutcome.getOutcome());
                    }
                }
                return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
            }
            default:{
                return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
            }
        }
    }
    
    private static ExpectedOutcome resolveExpectedOutcome(List<String> output, ExpectedOutcome expectedOutcome){
        try{
            switch (expectedOutcome.getBinaryRelation()){
                case EQUALS: return expectEquals(output, expectedOutcome);
                case NOT_EQUALS: return expectNotEquals(output, expectedOutcome);
                case CONTAINS: return expectContains(output, expectedOutcome);
                case NOT_CONTAINS: return expectDoesNotContain(output, expectedOutcome);
                case CONTAINED_BY: return expectContainedBy(output, expectedOutcome);
                case NOT_CONTAINED_BY: return expectNotContainedBy(output, expectedOutcome);
                case LESSER_THAN: return expectLesserThan(output, expectedOutcome);
                case LESSER_OR_EQUAL_TO: return expectLesserThanOrEqual(output, expectedOutcome);
                case GREATER_THAN: return expectGreaterThan(output, expectedOutcome);
                case GREATER_OR_EQUAL_TO: return expectGreaterThanOrEqual(output, expectedOutcome);
                default: return ExpectedOutcome.executionError(MessageLiterals.UnsuportedBinaryRelation);
            }
        }catch (NumberFormatException e){
            return ExpectedOutcome.executionError(MessageLiterals.ExpectedOutcomeNotNumeric);
        }
    }

    private static ExpectedOutcome expectEquals(List<String> output, ExpectedOutcome expectedOutcome){
        return expectedOutcome.withResolved(getStringRepresentation(output).equals(expectedOutcome.getExpectedOutput()));
    }

    private static ExpectedOutcome expectNotEquals(List<String> output, ExpectedOutcome expectedOutcome){
        return expectedOutcome.withResolved(!getStringRepresentation(output).equals(expectedOutcome.getExpectedOutput()));
    }

    private static ExpectedOutcome expectContains(List<String> output, ExpectedOutcome expectedOutcome){
        return expectedOutcome.withResolved(getStringRepresentation(output).contains(expectedOutcome.getExpectedOutput()));
    }

    private static ExpectedOutcome expectDoesNotContain(List<String> output, ExpectedOutcome expectedOutcome){
        return expectedOutcome.withResolved(!getStringRepresentation(output).contains(expectedOutcome.getExpectedOutput()));
    }

    private static ExpectedOutcome expectContainedBy(List<String> output, ExpectedOutcome expectedOutcome){
        return expectedOutcome.withResolved(expectedOutcome.getExpectedOutput().contains(getStringRepresentation(output)));
    }

    private static ExpectedOutcome expectNotContainedBy(List<String> output, ExpectedOutcome expectedOutcome){
        return expectedOutcome.withResolved(!expectedOutcome.getExpectedOutput().contains(getStringRepresentation(output)));
    }

    private static ExpectedOutcome expectLesserThan(List<String> output, ExpectedOutcome expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(getStringRepresentation(output));
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedOutput());
        return expectedOutcome.withResolved(outputAsDouble < expectedOutcomeAsDouble);
    }

    private static ExpectedOutcome expectLesserThanOrEqual(List<String> output, ExpectedOutcome expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(getStringRepresentation(output));
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedOutput());
        return expectedOutcome.withResolved(outputAsDouble <= expectedOutcomeAsDouble);
        
    }

    private static ExpectedOutcome expectGreaterThan(List<String> output, ExpectedOutcome expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(getStringRepresentation(output));
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedOutput());
        return expectedOutcome.withResolved(outputAsDouble > expectedOutcomeAsDouble);
    }

    private static ExpectedOutcome expectGreaterThanOrEqual(List<String> output, ExpectedOutcome expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(getStringRepresentation(output));
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedOutput());
        return expectedOutcome.withResolved(outputAsDouble >= expectedOutcomeAsDouble);
    }

    private static String getStringRepresentation(List<String> output){
        StringBuilder stringRepresentation = new StringBuilder();
        for(String line : output){
            stringRepresentation.append(line.trim()).append(" ");
        }
        return stringRepresentation.toString();
    }
}
