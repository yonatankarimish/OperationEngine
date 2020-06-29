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
    private static Map<String, CompletableFuture<OperationResult>> futureOutcomesByOperationId;

    @BeforeGroups(groups = "operation")
    public void initSpringBeans() {
        futureOutcomesByOperationId = new ConcurrentHashMap<>();
    }

    @AfterMethod(groups = "operation")
    public void engineTestCleanup(){
        diagnosticManager.clearDiagnosedSessions();
        futureOutcomesByOperationId.clear();
    }


    public static OperationResult executeOperation(Operation operation) throws AssertionError {
        submitOperation(operation);
        OperationResult resolvedOutcome = awaitOperation(operation);

        logger.info("Operation " + operation.getOperationName() + " Completed with result " + resolvedOutcome.getExpressionResult().getOutcome());
        logger.info("Result Message: " + resolvedOutcome.getExpressionResult().getMessage());
        return resolvedOutcome;
    }

    public static Session submitOperation(Operation operation){
        /* We use threadingManager.submit() because initializing sessions emits a SessionCreated event,
         * which logs a warning when invoked outside of a non-monitored thread*/
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

        try {
            Session session = preInitSession.get();
            CompletableFuture<OperationResult> operationResult = threadingManager.submit(() -> sessionEngine.executeOperation(session, operation));
            futureOutcomesByOperationId.put(operation.getUUID(), operationResult);
            return session;
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError(e); //equivalent to Assert.error(e), but does not trigger a compile-time exception
        }
    }

    public static OperationResult awaitOperation(Operation operation){
        return awaitOperation(operation, futureOutcomesByOperationId.get(operation.getUUID()));
    }

    private static OperationResult awaitOperation(Operation operation, CompletableFuture<OperationResult> runningOperation){
        /* We use the applyFutureCallback() because finalizing sessions emits a SessionClosed event,
         * which logs a warning when invoked outside of a non-monitored thread*/
        CompletableFuture<OperationResult> postTeardownOperation = threadingManager.applyFutureCallback(runningOperation, resolvedOutcome -> {
            /*if the operation id is not contained in runningOperations, it was terminated by invoking terminateOperation()
             * therefore, we don't need to finalize it*/
            if(sessionEngine.getRunningOperations().containsKey(operation.getUUID())) {
                try {
                    String sessionId = sessionEngine.getOperationsToSessions().get(operation.getUUID());
                    Session session = sessionEngine.getRunningSessions().get(sessionId);
                    sessionEngine.finalizeSession(session, operation.getUUID());
                } catch (IOException e) {
                    throw new RuntimeException(e); //rethrow as a runtime exception
                }
            }

            return resolvedOutcome;
        });

        try {
            return postTeardownOperation.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError(e); //equivalent to Assert.error(e), but does not trigger a compile-time exception
        }
    }

    public static OperationResult terminateOperation(Operation operation){
        //terminateOperation() calls finalizeSession(), so not additional care is needed
        try {
            return threadingManager.submit(() ->  sessionEngine.terminateOperation(operation.getUUID())).get();
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
