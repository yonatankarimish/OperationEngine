package com.sixsense.operation;

import com.sixsense.SixSenseBaseUtils;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.io.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class OperationTestUtils extends SixSenseBaseUtils {
    private static final Logger logger = LogManager.getLogger(OperationTestUtils.class);
    private static Map<String, Future<OperationResult>> futureOutcomesBySessionId;

    @BeforeGroups(groups = "operation")
    public void initSpringBeans() {
        futureOutcomesBySessionId = new ConcurrentHashMap<>();
    }

    @AfterMethod(groups = "operation")
    public void engineTestCleanup(){
        diagnosticManager.clearDiagnosedSessions();
        futureOutcomesBySessionId.clear();
    }


    public static OperationResult executeOperation(Operation operation) throws AssertionError {
        Session session = submitOperation(operation);
        OperationResult resolvedOutcome = awaitOperation(session);

        logger.info("Operation " + operation.getOperationName() + " Completed with result " + resolvedOutcome.getExpressionResult().getOutcome());
        logger.info("Result Message: " + resolvedOutcome.getExpressionResult().getMessage());
        return resolvedOutcome;
    }

    public static Session submitOperation(Operation operation){
        try {
            Session session = sessionEngine.initializeSession(operation);
            session.activateDebugMode();
            diagnosticManager.registerSession(session.getSessionShellId());
            Future<OperationResult> operationResult = threadingManager.submit(() -> sessionEngine.executeOperation(session, operation));
            futureOutcomesBySessionId.put(session.getSessionShellId(), operationResult);
            return session;
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            throw new NullPointerException(e.getMessage()); //should not be reached, as Assert.fail() will throw an assertion error
        }
    }

    public static OperationResult awaitOperation(Session session){
        return awaitOperation(session, futureOutcomesBySessionId.get(session.getSessionShellId()));
    }

    private static OperationResult awaitOperation(Session session, Future<OperationResult> runningOperation){
        OperationResult resolvedOutcome = null;
        try {
            resolvedOutcome = runningOperation.get();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            try {
                futureOutcomesBySessionId.remove(session.getSessionShellId());
                sessionEngine.finalizeSession(session);
            } catch (IOException ioex) {
                Assert.fail(ioex.getMessage());
            }
        }

        return resolvedOutcome;
    }

    //Will throw an assertion error if the future is not already resolved, and does not resolved within the specified time limit
    public static <V> V resolveWithin(Future<V> future, int seconds){
        try {
            return future.get(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Could not resolve future due to interruption exception", e);
        } catch (ExecutionException e) {
            Assert.fail("Could not resolve future due to execution exception", e);
        } catch (TimeoutException e) {
            Assert.fail("Could not resolve future due to timeout exception", e);
        }

        return null;
    }
}
