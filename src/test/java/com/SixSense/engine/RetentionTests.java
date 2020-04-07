package com.SixSense.engine;

import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.commands.*;
import com.SixSense.data.events.AbstractEngineEvent;
import com.SixSense.data.events.EngineEventType;
import com.SixSense.data.events.ResultRetentionEvent;
import com.SixSense.data.logic.*;
import com.SixSense.data.retention.OperationResult;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.RetentionType;
import com.SixSense.io.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Future;

//It might be worth to add test cases for the cartesian product of chaining any two ICommands
@Test(groups = {"engine"})
public class RetentionTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(RetentionTests.class);

    public void testRetentionToVariable(){
        ICommand simpleCommand = new Command()
                .withChannel(ChannelType.LOCAL)
                .withCommandText("echo lorem ipsum dolor sit amet")
                .withExpectedOutcome(
                    new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(
                        new ExpectedOutcome()
                        .withBinaryRelation(BinaryRelation.CONTAINS)
                        .withExpectedValue("lorem ipsum dolor sit amet")
                    )
                ).withSaveTo(
                    new ResultRetention()
                    .withRetentionType(RetentionType.Variable)
                    .withName("var.test.retention")
                );

        Operation operation = new Operation()
                .withOperationName("Test for retention to a variable")
                .withExecutionBlock(simpleCommand)
                .addChannel(ChannelType.LOCAL);

        Session session = EngineTestUtils.submitOperation(operation);
        try{
            Future<AbstractEngineEvent> textWrapper = EngineTestUtils.getDiagnosticManager().awaitAndConsume(session.getSessionShellId(), EngineEventType.ResultRetention);
            ResultRetentionEvent retentionEvent = (ResultRetentionEvent)EngineTestUtils.resolveWithin(textWrapper, 5);
            Assert.assertEquals(retentionEvent.getResultRetention().getName(), "var.test.retention");
            Assert.assertEquals(retentionEvent.getResultRetention().getValue(), "lorem ipsum dolor sit amet");
        }catch (ClassCastException e){
            Assert.fail("Failed to cast engine event. Caused by: ", e);
        }catch (NullPointerException e){
            Assert.fail("NullPointerException encountered. Caused by: ", e);
        }

        OperationResult operationResult = EngineTestUtils.awaitOperation(session);
        Assert.assertEquals(operationResult.getExpressionResult().getOutcome(), ResultStatus.SUCCESS);
        Assert.assertTrue(operationResult.getExpressionResult().isResolved());
    }
}
