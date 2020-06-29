package com.sixsense.operation;

import com.sixsense.SixSenseBaseTest;
import com.sixsense.model.commands.Block;
import com.sixsense.model.commands.Command;
import com.sixsense.model.commands.ICommand;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.events.AbstractEngineEvent;
import com.sixsense.model.events.EngineEventType;
import com.sixsense.model.events.InputSentEvent;
import com.sixsense.model.events.OutputReceivedEvent;
import com.sixsense.model.logic.*;
import com.sixsense.model.pipes.DrainingPipe;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.model.retention.RetentionMode;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.io.Session;
import com.sixsense.utillity.DynamicFieldGlossary;
import com.sixsense.utillity.InternalCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Future;

@Test(groups = {"operation"})
public class LocalOperationTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(LocalOperationTests.class);

    public void simpleLocalOperation() {
        ICommand localBlock = loopbackInterface()
            .chainCommands(this.localIp())
            .chainCommands(this.rxBytes())
            .chainCommands(this.txBytes())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withExpressionResult(new ExpressionResult().withMessage("Completed successfully"))
            );

        Operation simpleOperation = new Operation()
            .withOperationName("Network Details - Simple")
            .withExecutionBlock(localBlock)
            .addChannel(ChannelType.LOCAL);

        OperationResult operationResult = OperationTestUtils.executeOperation(simpleOperation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    public void simpleFailingOperation() {
        ICommand localBlock = loopbackInterface()
            .chainCommands(commandWithExpectedOutcomeNotReached())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withExpressionResult(new ExpressionResult().withMessage("Completed successfully"))
            );

        Operation failingOperation = new Operation()
            .withOperationName("Network Details - Planned failure")
            .withExecutionBlock(localBlock)
            .addChannel(ChannelType.LOCAL);

        OperationResult operationResult = OperationTestUtils.executeOperation(failingOperation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.FAILURE);
        Assert.assertFalse(operationResult.getExpressionResult().isResolved());
    }

    public void invalidOperation() {
        ICommand localBlock = loopbackInterface()
            .chainCommands(invalidCommand())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withExpressionResult(new ExpressionResult().withMessage("Completed successfully"))
            );

        Operation invalidOperation = new Operation()
            .withOperationName("Network Details - Planned failure")
            .withExecutionBlock(localBlock)
            .addChannel(ChannelType.LOCAL);

        OperationResult operationResult = OperationTestUtils.executeOperation(invalidOperation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.FAILURE);
        Assert.assertFalse(operationResult.getExpressionResult().isResolved());
    }

    public void nestedBlock() {
        Block parentBlock = (Block) new Block().withExpectedOutcome(
            new LogicalExpression<ExpectedOutcome>()
                .withExpressionResult(new ExpressionResult().withMessage("Parent block completed successfully"))
        );

        for (int i = 1; i <= 3; i++) {
            String blockID = "block-" + i;
            Block commandBlock = (Block) new Block()
                .addDynamicField(DynamicFieldGlossary.var_block_id, blockID + "-should-get-overridden")
                .withExpectedOutcome(
                    new LogicalExpression<ExpectedOutcome>()
                        .withExpressionResult(new ExpressionResult().withMessage("block-" + i + " completed successfully"))
                );

            for (int j = 1; j <= 3; j++) {
                String commandID = "command-" + j;
                Command blockPart = blockPartCommand(blockID, commandID);
                commandBlock.addChildBlock(blockPart);
            }
            parentBlock.addChildBlock(commandBlock);
        }

        Operation blockOperation = (Operation) new Operation()
            .withOperationName("Block testing - three nested blocks, three commands each")
            .withExecutionBlock(parentBlock)
            .addChannel(ChannelType.LOCAL)
            .addDynamicField(DynamicFieldGlossary.var_operation_name, "Three nested blocks")
            .addDynamicField(DynamicFieldGlossary.var_operation_vendor, "Linux")
            .addDynamicField(DynamicFieldGlossary.var_operation_product, "CentOS")
            .addDynamicField(DynamicFieldGlossary.var_operation_version, "6");

        OperationResult operationResult = OperationTestUtils.executeOperation(blockOperation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    public void repeatingBlock() {
        ICommand echoValue = new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo $var.block.counter")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(5)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.EQUALS)
                        .withExpectedValue("$var.block.counter")
                )
            );
        ICommand increment = InternalCommands.assignValue("var.block.counter", "$var.block.counter + 1");

        Block repeatingBlock = ((Block) echoValue.chainCommands(increment))
            .withRepeatCondition(
                new LogicalExpression<ExecutionCondition>().addResolvable(
                    new ExecutionCondition()
                        .withVariable("$var.block.counter")
                        .withBinaryRelation(BinaryRelation.LESSER_OR_EQUAL_TO)
                        .withExpectedValue("$var.block.repeatCount")
                )
            );

        Operation repeatingOperation = (Operation) new Operation()
            .withOperationName("Block testing - repeating block")
            .withExecutionBlock(repeatingBlock)
            .addChannel(ChannelType.LOCAL)
            .addDynamicField(DynamicFieldGlossary.var_block_repeatCount, "5")
            .addDynamicField(DynamicFieldGlossary.var_block_counter, "1");

        OperationResult operationResult = OperationTestUtils.executeOperation(repeatingOperation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    public void fileWriteOperation() {
        ICommand fileWrite = new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("cat /etc/hosts")
            .withSecondsToTimeout(90)
            .withUseRawOutput(true)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("cat")
                    )
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("$sixsense.session.prompt.local")
                    )
            ).withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.File)
                    .withName("hosts.txt")
            );

        Operation writeOperation = new Operation()
            .withOperationName("Network details - Write to file")
            .withExecutionBlock(fileWrite)
            .addChannel(ChannelType.LOCAL);

        OperationResult operationResult = OperationTestUtils.executeOperation(writeOperation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    public void drainingFileOperation() {
        Operation writeOperation = new Operation()
            .withOperationName("Network Details - File parsing")
            .withExecutionBlock(drainingFileCopy())
            .addChannel(ChannelType.LOCAL)
            .addChannel(ChannelType.DOWNLOAD);

        OperationResult operationResult = OperationTestUtils.executeOperation(writeOperation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    public void executeIfConditionsAreMet() {
        LogicalExpression<ExecutionCondition> reallyComplexCondition = new LogicalExpression<ExecutionCondition>()
            .withLogicalCondition(LogicalCondition.AND)
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.phrase")
                .withBinaryRelation(BinaryRelation.CONTAINS)
                .withExpectedValue("$var.operation.brownFox")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.phrase")
                .withBinaryRelation(BinaryRelation.NOT_CONTAINS)
                .withExpectedValue("$var.operation.greenMantis")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.brownFox")
                .withBinaryRelation(BinaryRelation.CONTAINED_BY)
                .withExpectedValue("$var.operation.phrase")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.greenMantis")
                .withBinaryRelation(BinaryRelation.NOT_CONTAINED_BY)
                .withExpectedValue("$var.operation.phrase")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.smallNumber")
                .withBinaryRelation(BinaryRelation.LESSER_THAN)
                .withExpectedValue("$var.operation.bigNumber")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.bigNumber")
                .withBinaryRelation(BinaryRelation.LESSER_OR_EQUAL_TO)
                .withExpectedValue("$var.operation.bigNumber")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.bigNumber")
                .withBinaryRelation(BinaryRelation.EQUALS)
                .withExpectedValue("$var.operation.bigNumber")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.bigNumber")
                .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                .withExpectedValue("$var.operation.smallNumber")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.smallNumber")
                .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO)
                .withExpectedValue("$var.operation.smallNumber")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.bigNumber")
                .withBinaryRelation(BinaryRelation.GREATER_THAN)
                .withExpectedValue("$var.operation.smallNumber")
            )
            .addResolvable(new ExecutionCondition()
                .withVariable("$var.operation.bigNumber")
                .withBinaryRelation(BinaryRelation.GREATER_THAN)
                .withExpectedValue("$var.operation.smallNumber")
            );

        Operation writeOperation = (Operation)new Operation()
            .withOperationName("Execute operation - conditions are met")
            .withExecutionBlock(
                new Block()
                .addChildBlock(
                    new Command()
                    .withCommandText("echo condition stack finished!")
                    .withChannel(ChannelType.LOCAL)
                    .withExecutionCondition(reallyComplexCondition)
                        .addDynamicField("var.operation.phrase", "the slow brown fox misses the fast lucky rabbit")
                        .addDynamicField("var.operation.brownFox", "brown fox")
                        .addDynamicField("var.operation.greenMantis", "green mantis")
                        .addDynamicField("var.operation.smallNumber", "75")
                        .addDynamicField("var.operation.bigNumber", "85")
                    .withExpectedOutcome(
                        new LogicalExpression<ExpectedOutcome>()
                        .addResolvable(
                            new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("condition stack finished!")
                        )
                    )
                    .withSaveTo(
                        new ResultRetention()
                        .withRetentionMode(RetentionMode.Variable)
                        .withName("var.command.finished")
                    )
                )
                .withExecutionCondition(reallyComplexCondition)
                .addDynamicField("var.operation.phrase", "the fierce brown fox eats the poor little rabbit")
                .addDynamicField("var.operation.brownFox", "brown fox")
                .addDynamicField("var.operation.greenMantis", "green mantis")
                .addDynamicField("var.operation.smallNumber", "4")
                .addDynamicField("var.operation.bigNumber", "5")
            )
            .addChannel(ChannelType.LOCAL)
            .addDynamicField("var.operation.phrase", "the quick brown fox jumps over the lazy dog")
            .addDynamicField("var.operation.brownFox", "brown fox")
            .addDynamicField("var.operation.greenMantis", "green mantis")
            .addDynamicField("var.operation.smallNumber", "1")
            .addDynamicField("var.operation.bigNumber", "999")
            .withExecutionCondition(reallyComplexCondition);

        OperationResult operationResult = OperationTestUtils.executeOperation(writeOperation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    public void dynamicFieldLoading(){
        Command command = (Command)new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo " + DynamicFieldGlossary.$(DynamicFieldGlossary.var_cmd_text) + " " + DynamicFieldGlossary.$(DynamicFieldGlossary.var_stack_text))
            .addDynamicField(DynamicFieldGlossary.var_cmd_text, "command")
            .addDynamicField(DynamicFieldGlossary.var_stack_text, "command")
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                .addResolvable(new ExpectedOutcome()
                    .withBinaryRelation(BinaryRelation.CONTAINS)
                    .withExpectedValue("command command")
                )
            );

        Block block = (Block)new Block()
            .addChildBlock(command)
            .addDynamicField(DynamicFieldGlossary.var_stack_text, "block");

        Operation operation = (Operation)new Operation()
            .withOperationName("Dynamic field loading")
            .withExecutionBlock(block)
            .addChannel(ChannelType.LOCAL)
            .addDynamicField(DynamicFieldGlossary.var_stack_text, "operation");

        Session session = OperationTestUtils.submitOperation(operation);
        try{
            Future<AbstractEngineEvent> textWrapper = OperationTestUtils.getDiagnosticManager().awaitAndConsume(session.getSessionShellId(), EngineEventType.InputSent);
            InputSentEvent sentRightText = (InputSentEvent) OperationTestUtils.resolveWithin(textWrapper, 5);
            Assert.assertEquals(sentRightText.getInputSent(), "echo command command");

            Future<AbstractEngineEvent> outputWrapper = OperationTestUtils.getDiagnosticManager().awaitAndConsume(session.getSessionShellId(), EngineEventType.OutputReceived);
            OutputReceivedEvent receivedRightText = (OutputReceivedEvent) OperationTestUtils.resolveWithin(outputWrapper, 5);
            Assert.assertEquals(receivedRightText.getOutputReceived(), "command command");
        }catch (ClassCastException e){
            Assert.fail("Failed to cast engine event. Caused by: ", e);
        }catch (NullPointerException e){
            Assert.fail("NullPointerException encountered. Caused by: ", e);
        }

        OperationResult operationResult = OperationTestUtils.awaitOperation(operation);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    public void verifyTerminationWhileWaitingForResponse() {
        Command waitingCommand = ((Command)loopbackInterface())
            .withMinimalSecondsToResponse(15); //if we wait for the full 15 seconds, then the operation definitely did not abort the sleep period

        Operation operation = new Operation()
            .withOperationName("Verify termination during minimal seconds to response")
            .withExecutionBlock(waitingCommand)
            .addChannel(ChannelType.LOCAL);

        Instant start = Instant.now();
        OperationTestUtils.submitOperation(operation);
        OperationTestUtils.terminateOperation(operation);
        OperationResult operationResult = OperationTestUtils.awaitOperation(operation);
        Instant end = Instant.now();

        long elapsedSeconds = start.until(end, ChronoUnit.SECONDS);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.FAILURE);
        Assert.assertTrue(elapsedSeconds < waitingCommand.getMinimalSecondsToResponse());
    }

    public void verifyTerminationWhileWaitingForTimeout() {
        Command waitingCommand = ((Command)loopbackInterface())
            .withMinimalSecondsToResponse(0)
            .withSecondsToTimeout(15); //if we wait for the full 15 seconds, then the operation definitely did not abort the timeout period

        Operation operation = new Operation()
            .withOperationName("Verify termination during seconds to timeout")
            .withExecutionBlock(waitingCommand)
            .addChannel(ChannelType.LOCAL);

        Instant start = Instant.now();
        OperationTestUtils.submitOperation(operation);
        OperationTestUtils.terminateOperation(operation);
        OperationResult operationResult = OperationTestUtils.awaitOperation(operation);
        Instant end = Instant.now();

        long elapsedSeconds = start.until(end, ChronoUnit.SECONDS);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.FAILURE);
        Assert.assertTrue(elapsedSeconds < waitingCommand.getSecondsToTimeout());
    }

    //TODO: find a way to generalize these tests for each qa machine
    private ICommand loopbackInterface() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig | grep 'Loopback'")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("Loopback")
                        .withExpressionResult(new ExpressionResult().withMessage("The correct interface name was found"))
                )
            );
    }

    private ICommand localIp() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig | grep 'inet addr' | head -n 2 | tail -1")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("127.0.0.1")
                        .withExpressionResult(new ExpressionResult().withMessage("The correct IP address is defined for the interface"))
                )
            );
    }

    private ICommand rxBytes() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig lo | grep 'RX bytes' | awk '{print $2}' | sed 's/bytes://g'")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO)
                        .withExpectedValue("1000")
                        .withExpressionResult(new ExpressionResult().withMessage("More than 1kb was sent through this interface"))
                )
            );
    }

    private ICommand txBytes() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig lo | grep 'TX bytes' | awk '{print $2}' | sed 's/bytes://g'")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.GREATER_OR_EQUAL_TO)
                        .withExpectedValue("1000")
                        .withExpressionResult(new ExpressionResult().withMessage("More than 1kb was received through this interface"))
                )
            );
    }

    private ICommand commandWithExpectedOutcomeNotReached() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("ifconfig lo | grep 'TX bytes'")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("notPartOfCat")
                        .withExpressionResult(new ExpressionResult().withMessage("no byte count should not be here"))
                )
            );
    }

    private ICommand invalidCommand() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("dsfhjk error on purpose")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>().addResolvable(
                    new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("docker")
                        .withExpressionResult(new ExpressionResult().withMessage("no valid response should be here"))
                )
            );
    }

    private ICommand drainingFileCopy() {
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("cat /var/log/syslog")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(45)
            .withUseRawOutput(true)
            .addOutputPipe(new DrainingPipe())
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.ENDS_WITH)
                            .withExpectedValue("$sixsense.session.prompt.local")
                    )
            )
            .withSaveTo(
                new ResultRetention()
                    .withRetentionMode(RetentionMode.File)
                    .withName("iptable.daily.log")
            );
    }

    private Command blockPartCommand(String blockID, String commandID) {
        return (Command) new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo $var.block.id $var.command.id")
            .withMinimalSecondsToResponse(1)
            .withSecondsToTimeout(10)
            .addDynamicField(DynamicFieldGlossary.var_block_id, blockID)
            .addDynamicField(DynamicFieldGlossary.var_command_id, commandID)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .withLogicalCondition(LogicalCondition.AND)
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue(blockID)
                        .withExpressionResult(new ExpressionResult().withMessage("Block ID has been matched"))
                    )
                    .addResolvable(new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue(commandID)
                        .withExpressionResult(new ExpressionResult().withMessage("Command ID has been matched"))
                    )
                    .withExpressionResult(new ExpressionResult().withMessage("Command params found in response"))
            );
    }
}
