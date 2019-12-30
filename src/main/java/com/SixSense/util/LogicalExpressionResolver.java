package com.SixSense.util;

import com.SixSense.data.logic.*;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicalExpressionResolver {
    //resolves logical expressions composed of execution conditions
    public static ExpressionResult resolveLogicalExpression(Map<String, String> sessionFields, LogicalExpression<? extends IResolvable> logicalExpression){
        return resolveLogicalExpression("", sessionFields, logicalExpression);
    }

    //resolves logical expressions composed of expected outcomes
    public static ExpressionResult resolveLogicalExpression(String commandOutput, Map<String, String> sessionFields, LogicalExpression<? extends IResolvable> logicalExpression){
        if(logicalExpression.getResolvableExpressions().isEmpty()){
            return ExpressionResult.defaultOutcome();
        }

        ExpressionResult expressionResult;
        switch (logicalExpression.getLogicalCondition()) {
            case OR: {
                for (IResolvable resolvable : logicalExpression.getResolvableExpressions()) {
                    expressionResult = resolve(commandOutput, sessionFields, resolvable);
                    if (expressionResult.isResolved()) {
                        return expressionResult;
                    }
                }
                return ExpressionResult.executionError(MessageLiterals.CommandDidNotReachOutcome);
            }
            case NOR: {
                for (IResolvable resolvable : logicalExpression.getResolvableExpressions()) {
                    expressionResult = resolve(commandOutput, sessionFields, resolvable);
                    if (expressionResult.isResolved()) {
                        return ExpressionResult.executionError(MessageLiterals.CommandDidNotReachOutcome);
                    }
                }
                return ExpressionResult.defaultOutcome();
            }
            case AND: {
                for (IResolvable resolvable : logicalExpression.getResolvableExpressions()) {
                    expressionResult = resolve(commandOutput, sessionFields, resolvable);
                    if (!expressionResult.isResolved()) {
                        return ExpressionResult.executionError(MessageLiterals.CommandDidNotReachOutcome);
                    }
                }
                return ExpressionResult.defaultOutcome();
            }
            case NAND: {
                for (IResolvable resolvable : logicalExpression.getResolvableExpressions()) {
                    expressionResult = resolve(commandOutput, sessionFields, resolvable);
                    if (!expressionResult.isResolved()) {
                        return expressionResult;
                    }
                }
                return ExpressionResult.executionError(MessageLiterals.CommandDidNotReachOutcome);
            }
            default: {
                return ExpressionResult.executionError(MessageLiterals.UnsuportedBinaryRelation);
            }
        }
    }

    private static ExpressionResult resolve(String commandOutput, Map<String, String> sessionFields, IResolvable resolvable){
        if(resolvable instanceof ExecutionCondition){
            return evaluateAndResolveCondition((ExecutionCondition)resolvable, sessionFields);
        }else if(resolvable instanceof ExpectedOutcome) {
            return evaluateAndResolveOutcome(commandOutput, (ExpectedOutcome)resolvable, sessionFields);
        }else if(resolvable instanceof LogicalExpression){
            return resolveLogicalExpression(commandOutput, sessionFields, (LogicalExpression<? extends IResolvable>)resolvable);
        }
        throw new IllegalArgumentException("Cannot resolve expressions of type " + resolvable.getClass().getSimpleName());
    }

    private static ExpressionResult evaluateAndResolveCondition(ExecutionCondition executionCondition, Map<String, String> sessionFields){
        String conditionInput = CommandUtils.evaluateAgainstDynamicFields(executionCondition.getVariable(), sessionFields);
        String conditionValue = CommandUtils.evaluateAgainstDynamicFields(executionCondition.getExpectedValue(), sessionFields);
        ExecutionCondition withExpectedValue = executionCondition.deepClone().withExpectedValue(conditionValue);

        return LogicalExpressionResolver.resolveBinaryOperand(conditionInput, withExpectedValue);
    }

    private static ExpressionResult evaluateAndResolveOutcome(String commandOutput, ExpectedOutcome possibleOutcome, Map<String, String> sessionFields){
        String outcomeValue = CommandUtils.evaluateAgainstDynamicFields(possibleOutcome.getExpectedValue(), sessionFields);
        ExpectedOutcome withExpectedValue = possibleOutcome.deepClone().withExpectedValue(outcomeValue);

        return LogicalExpressionResolver.resolveBinaryOperand(commandOutput, withExpectedValue);
    }
    
    private static ExpressionResult resolveBinaryOperand(String output, IFlowConnector expectedOutcome){
        try{
            boolean evaluation;
            switch (expectedOutcome.getBinaryRelation()){
                case EQUALS: evaluation = expectEquals(output, expectedOutcome); break;
                case NOT_EQUALS: evaluation = expectNotEquals(output, expectedOutcome); break;
                case CONTAINS: evaluation = expectContains(output, expectedOutcome); break;
                case NOT_CONTAINS: evaluation = expectDoesNotContain(output, expectedOutcome); break;
                case CONTAINED_BY: evaluation = expectContainedBy(output, expectedOutcome); break;
                case NOT_CONTAINED_BY: evaluation = expectNotContainedBy(output, expectedOutcome); break;
                case LESSER_THAN: evaluation = expectLesserThan(output, expectedOutcome); break;
                case LESSER_OR_EQUAL_TO: evaluation = expectLesserThanOrEqual(output, expectedOutcome); break;
                case GREATER_THAN: evaluation = expectGreaterThan(output, expectedOutcome); break;
                case GREATER_OR_EQUAL_TO: evaluation = expectGreaterThanOrEqual(output, expectedOutcome); break;
                case MATCHES_REGEX: evaluation = expectMatchesRegex(output, expectedOutcome); break;
                default: return ExpressionResult.executionError(MessageLiterals.UnsuportedBinaryRelation);
            }

            return expectedOutcome.getExpressionResult().withResolved(evaluation);
        }catch (NumberFormatException e){
            return ExpressionResult.executionError(MessageLiterals.ExpectedOutcomeNotNumeric);
        }
    }

    private static boolean expectEquals(String output, IFlowConnector expectedOutcome){
        return output.equals(expectedOutcome.getExpectedValue());
    }

    private static boolean expectNotEquals(String output, IFlowConnector expectedOutcome){
        return !output.equals(expectedOutcome.getExpectedValue());
    }

    private static boolean expectContains(String output, IFlowConnector expectedOutcome){
        return output.contains(expectedOutcome.getExpectedValue());
    }

    private static boolean expectDoesNotContain(String output, IFlowConnector expectedOutcome){
        return !output.contains(expectedOutcome.getExpectedValue());
    }

    private static boolean expectContainedBy(String output, IFlowConnector expectedOutcome){
        return expectedOutcome.getExpectedValue().contains(output);
    }

    private static boolean expectNotContainedBy(String output, IFlowConnector expectedOutcome){
        return !expectedOutcome.getExpectedValue().contains(output);
    }

    private static boolean expectLesserThan(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(output);
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedValue());
        return outputAsDouble < expectedOutcomeAsDouble;
    }

    private static boolean expectLesserThanOrEqual(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(output);
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedValue());
        return outputAsDouble <= expectedOutcomeAsDouble;
        
    }

    private static boolean expectGreaterThan(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(output);
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedValue());
        return outputAsDouble > expectedOutcomeAsDouble;
    }

    private static boolean expectGreaterThanOrEqual(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        double outputAsDouble = Double.valueOf(output);
        double expectedOutcomeAsDouble = Double.valueOf(expectedOutcome.getExpectedValue());
        return outputAsDouble >= expectedOutcomeAsDouble;
    }

    private static boolean expectMatchesRegex(String output, IFlowConnector expectedOutcome) throws NumberFormatException {
        Pattern expectedPattern = Pattern.compile(expectedOutcome.getExpectedValue());
        Matcher patternMatcher = expectedPattern.matcher(output);
        return patternMatcher.find();
    }

}
