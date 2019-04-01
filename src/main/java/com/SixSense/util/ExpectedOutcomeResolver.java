package com.SixSense.util;

import com.SixSense.data.outcomes.ExecutionCondition;
import com.SixSense.data.outcomes.ExpectedOutcome;
import com.SixSense.data.outcomes.IFlowConnector;
import com.SixSense.data.outcomes.LogicalCondition;

import java.util.List;
import java.util.Map;

import static com.SixSense.util.MessageLiterals.CommandDidNotReachOutcome;
import static com.SixSense.util.MessageLiterals.UnsuportedBinaryRelation;

public class ExpectedOutcomeResolver {
    public static ExecutionCondition checkExecutionConditions(Map<String, String> sessionFields, List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation){
        //If the execution conditions are empty, then no conditions must be met. Therefore - return a matching success
        if(executionConditions == null || executionConditions.isEmpty()){
            return ExecutionCondition.matchingSuccess();
        }

        ExecutionCondition resolvedCondition;
        switch (conditionAggregation) {
            case OR:{
                for(ExecutionCondition executionCondition : executionConditions){
                    resolvedCondition = (ExecutionCondition)ExpectedOutcomeResolver.resolveBinaryOperand(sessionFields.get(executionCondition.getVariable()), executionCondition);
                    if (resolvedCondition.isResolved()) {
                        return resolvedCondition;
                    }
                }
                return ExecutionCondition.matchingFailure();
            }
            case NOR:{
                for (ExecutionCondition executionCondition : executionConditions) {
                    resolvedCondition = (ExecutionCondition)ExpectedOutcomeResolver.resolveBinaryOperand(sessionFields.get(executionCondition.getVariable()), executionCondition);
                    if (resolvedCondition.isResolved()) {
                        return ExecutionCondition.matchingFailure();
                    }
                }
                return ExecutionCondition.matchingSuccess();
            }
            case AND:{
                for (ExecutionCondition executionCondition : executionConditions) {
                    resolvedCondition = (ExecutionCondition)ExpectedOutcomeResolver.resolveBinaryOperand(sessionFields.get(executionCondition.getVariable()), executionCondition);
                    if (!resolvedCondition.isResolved()) {
                        return ExecutionCondition.matchingFailure();
                    }
                }
                return ExecutionCondition.matchingSuccess();
            }
            case NAND:{
                for (ExecutionCondition executionCondition : executionConditions) {
                    resolvedCondition = (ExecutionCondition)ExpectedOutcomeResolver.resolveBinaryOperand(sessionFields.get(executionCondition.getVariable()), executionCondition);
                    if (!resolvedCondition.isResolved()) {
                        return resolvedCondition;
                    }
                }
                return ExecutionCondition.matchingFailure();
            }
            default:{
                return ExecutionCondition.matchingFailure();
            }
        }
    }

    public static ExpectedOutcome resolveExpectedOutcome(List<String> commandOutout, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation){
        String outputAsString = getStringRepresentation(commandOutout);
        ExpectedOutcome resolvedOutcome;
        switch (outcomeAggregation) {
            case OR: {
                for (ExpectedOutcome possibleOutcome : expectedOutcomes) {
                    resolvedOutcome = (ExpectedOutcome) ExpectedOutcomeResolver.resolveBinaryOperand(outputAsString, possibleOutcome);
                    if (resolvedOutcome.isResolved()) {
                        return resolvedOutcome;
                    }
                }
                return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
            }
            case NOR: {
                for (ExpectedOutcome possibleOutcome : expectedOutcomes) {
                    resolvedOutcome = (ExpectedOutcome) ExpectedOutcomeResolver.resolveBinaryOperand(outputAsString, possibleOutcome);
                    if (resolvedOutcome.isResolved()) {
                        return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
                    }
                }
                return ExpectedOutcome.defaultOutcome();
            }
            case AND: {
                for (ExpectedOutcome possibleOutcome : expectedOutcomes) {
                    resolvedOutcome = (ExpectedOutcome) ExpectedOutcomeResolver.resolveBinaryOperand(outputAsString, possibleOutcome);
                    if (!resolvedOutcome.isResolved()) {
                        return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
                    }
                }
                return ExpectedOutcome.defaultOutcome();
            }
            case NAND: {
                for (ExpectedOutcome possibleOutcome : expectedOutcomes) {
                    resolvedOutcome = (ExpectedOutcome) ExpectedOutcomeResolver.resolveBinaryOperand(outputAsString, possibleOutcome);
                    if (!resolvedOutcome.isResolved()) {
                        return resolvedOutcome;
                    }
                }
                return ExpectedOutcome.executionError(CommandDidNotReachOutcome);
            }
            default: {
                return ExpectedOutcome.executionError(UnsuportedBinaryRelation);
            }
        }
    }
    
    private static IFlowConnector resolveBinaryOperand(String output, IFlowConnector expectedOutcome){
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
                default: return ExpectedOutcome.executionError(UnsuportedBinaryRelation);
            }
        }catch (NumberFormatException e){
            if(expectedOutcome instanceof ExecutionCondition) {
                return ExecutionCondition.matchingFailure();
            }else{
                return ExpectedOutcome.executionError(MessageLiterals.ExpectedOutcomeNotNumeric);
            }
        }
    }

    private static IFlowConnector expectEquals(String output, IFlowConnector expectedOutcome){
        return expectedOutcome.withResolved(output.equals(expectedOutcome.getExpectedValue()));
    }

    private static IFlowConnector expectNotEquals(String output, IFlowConnector expectedOutcome){
        return expectedOutcome.withResolved(!output.equals(expectedOutcome.getExpectedValue()));
    }

    private static IFlowConnector expectContains(String output, IFlowConnector expectedOutcome){
        return expectedOutcome.withResolved(output.contains(expectedOutcome.getExpectedValue()));
    }

    private static IFlowConnector expectDoesNotContain(String output, IFlowConnector expectedOutcome){
        return expectedOutcome.withResolved(!output.contains(expectedOutcome.getExpectedValue()));
    }

    private static IFlowConnector expectContainedBy(String output, IFlowConnector expectedOutcome){
        return expectedOutcome.withResolved(expectedOutcome.getExpectedValue().contains(output));
    }

    private static IFlowConnector expectNotContainedBy(String output, IFlowConnector expectedOutcome){
        return expectedOutcome.withResolved(!expectedOutcome.getExpectedValue().contains(output));
    }

    private static IFlowConnector expectLesserThan(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(output);
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedValue());
        return expectedOutcome.withResolved(outputAsDouble < expectedOutcomeAsDouble);
    }

    private static IFlowConnector expectLesserThanOrEqual(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(output);
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedValue());
        return expectedOutcome.withResolved(outputAsDouble <= expectedOutcomeAsDouble);
        
    }

    private static IFlowConnector expectGreaterThan(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(output);
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedValue());
        return expectedOutcome.withResolved(outputAsDouble > expectedOutcomeAsDouble);
    }

    private static IFlowConnector expectGreaterThanOrEqual(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(output);
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedValue());
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
