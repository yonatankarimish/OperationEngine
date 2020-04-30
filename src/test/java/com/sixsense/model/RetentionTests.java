package com.sixsense.model;

import com.sixsense.SixSenseBaseTest;
import com.sixsense.SixSenseBaseUtils;
import com.sixsense.model.commands.*;
import com.sixsense.model.events.AbstractEngineEvent;
import com.sixsense.model.events.EngineEventType;
import com.sixsense.model.events.ResultRetentionEvent;
import com.sixsense.model.logic.*;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.model.retention.RetentionType;
import com.sixsense.io.Session;
import com.sixsense.operation.OperationTestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Future;

@Test(groups = {"model"})
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
