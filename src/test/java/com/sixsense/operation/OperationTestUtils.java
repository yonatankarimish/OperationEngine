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
    private static Map<String, CompletableFuture<OperationResult>> futureOutcomesBySessionId;

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
        /*We split the future session from the operation execution for two reasons:
        * 1) better granularity and control over failures
        * 2) initializing sessions emits a SessionCreated event, which logs a warning when invoked outside of a non-monitored thread*/
        CompletableFuture<Session> preInitSession = threadingManager.submit(() -> {
            try {
                Session session = sessionEngine.initializeSession(operation);
                session.activateDebugMode();
                diagnosticManager.registerSession(session.getSessionShellId());
                return session;
            } catch (InstantiationException e) {
                throw new RuntimeException(e); //rethrow as a runtime exception
            }
        });

        CompletableFuture<OperationResult> operationResult = threadingManager.applyFutureCallback(preInitSession,
            session -> sessionEngine.executeOperation(session, operation)
        );

        try {
            Session session = preInitSession.get();
            futureOutcomesBySessionId.put(session.getSessionShellId(), operationResult);
            return session;
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError(e); //equivalent to Assert.error(e), but does not trigger a compile-time exception
        }
    }

    public static OperationResult awaitOperation(Session session){
        return awaitOperation(session, futureOutcomesBySessionId.get(session.getSessionShellId()));
    }

    private static OperationResult awaitOperation(Session session, CompletableFuture<OperationResult> runningOperation){
        /*We add the thenApply() block for two reasons:
         * 1) ensure resolved outcomes are always removed from futureOutcomesBySessionId
         * 2) finalizing sessions emits a SessionClosed event, which logs a warning when invoked outside of a non-monitored thread*/
        CompletableFuture<OperationResult> postTeardownOperation = threadingManager.applyFutureCallback(runningOperation, resolvedOutcome -> {
            futureOutcomesBySessionId.remove(session.getSessionShellId());

            try {
                sessionEngine.finalizeSession(session);
            } catch (IOException e) {
                throw new RuntimeException(e); //rethrow as a runtime exception
            }

            return resolvedOutcome;
        });

        try {
            return postTeardownOperation.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError(e); //equivalent to Assert.error(e), but does not trigger a compile-time exception
        }
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
