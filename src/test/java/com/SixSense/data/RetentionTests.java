package com.SixSense.data;

import com.SixSense.SixSenseBaseTest;
import com.SixSense.SixSenseBaseUtils;
import com.SixSense.data.commands.*;
import com.SixSense.data.events.AbstractEngineEvent;
import com.SixSense.data.events.EngineEventType;
import com.SixSense.data.events.ResultRetentionEvent;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.OperationResult;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.RetentionType;
import com.SixSense.io.Session;
import com.SixSense.operation.OperationTestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Future;

@Test(groups = {"data"})
public class RetentionTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(RetentionTests.class);

    public void testRetentionToVariable(){
        ICommand simpleCommand = simpleRetainingCommand()
                .withSaveTo(
                    new ResultRetention()
                    .withRetentionType(RetentionType.Variable)
                    .withName("var.test.retention")
                );

        Operation operation = new Operation()
                .withOperationName("Test retention to a variable")
                .withExecutionBlock(simpleCommand)
                .addChannel(ChannelType.LOCAL);

        Session session = OperationTestUtils.submitOperation(operation);
        try{
            Future<AbstractEngineEvent> textWrapper = SixSenseBaseUtils.getDiagnosticManager().awaitAndConsume(session.getSessionShellId(), EngineEventType.ResultRetention);
            ResultRetentionEvent retentionEvent = (ResultRetentionEvent) OperationTestUtils.resolveWithin(textWrapper, 5);
            ResultRetention resultRetention = retentionEvent.getResultRetention();

            Assert.assertNotNull(resultRetention);
            Assert.assertEquals(resultRetention.getRetentionType(), RetentionType.Variable);
            Assert.assertEquals(resultRetention.getName(), "var.test.retention");
            Assert.assertEquals(resultRetention.getValue(), "lorem ipsum dolor sit amet");
        }catch (ClassCastException e){
            Assert.fail("Failed to cast engine event. Caused by: ", e);
        }catch (NullPointerException e){
            Assert.fail("NullPointerException encountered. Caused by: ", e);
        }

        OperationResult operationResult = OperationTestUtils.awaitOperation(session);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    public void testRetentionToDatabaseEventual(){
        ICommand simpleCommand = simpleRetainingCommand()
                .withSaveTo(
                    new ResultRetention()
                        .withRetentionType(RetentionType.DatabaseEventual)
                        .withName("var.test.retention")
                );

        Operation operation = new Operation()
                .withOperationName("Test retention to database (eventual)")
                .withExecutionBlock(simpleCommand)
                .addChannel(ChannelType.LOCAL);

        Session session = OperationTestUtils.submitOperation(operation);
        try{
            Future<AbstractEngineEvent> textWrapper = SixSenseBaseUtils.getDiagnosticManager().awaitAndConsume(session.getSessionShellId(), EngineEventType.ResultRetention);
            ResultRetentionEvent retentionEvent = (ResultRetentionEvent) OperationTestUtils.resolveWithin(textWrapper, 5);
            ResultRetention resultRetention = retentionEvent.getResultRetention();

            Assert.assertNotNull(resultRetention);
            Assert.assertEquals(resultRetention.getRetentionType(), RetentionType.DatabaseEventual);
            Assert.assertEquals(resultRetention.getName(), "var.test.retention");
            Assert.assertEquals(resultRetention.getValue(), "lorem ipsum dolor sit amet");
        }catch (ClassCastException e){
            Assert.fail("Failed to cast engine event. Caused by: ", e);
        }catch (NullPointerException e){
            Assert.fail("NullPointerException encountered. Caused by: ", e);
        }

        OperationResult operationResult = OperationTestUtils.awaitOperation(session);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }

    private Command simpleRetainingCommand(){
        return (Command)new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo lorem ipsum dolor sit amet")
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("lorem ipsum dolor sit amet")
                    )
            );
    }
}
