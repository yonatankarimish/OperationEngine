package com.SixSense.engine;

import com.SixSense.Main;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.io.Session;
import com.SixSense.queue.WorkerQueue;
import com.SixSense.util.EventQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class EngineTestUtils {
    private static final Logger logger = LogManager.getLogger(EngineTestUtils.class);
    private static ConfigurableApplicationContext appContext;
    private static SessionEngine sessionEngine;
    private static DiagnosticManager diagnosticManager;
    private static WorkerQueue workerQueue;

    private static Map<String, Future<ExpressionResult>> futureOutcomesBySessionId;

    @BeforeGroups("engine")
    public void beforeClass() {
        appContext = SpringApplication.run(Main.class);
        sessionEngine = (SessionEngine)appContext.getBean("sessionEngine");
        diagnosticManager = (DiagnosticManager)appContext.getBean("diagnosticManager");
        workerQueue = (WorkerQueue)appContext.getBean("workerQueue");

        futureOutcomesBySessionId = new ConcurrentHashMap<>();
    }

    public static void engineTestCleanup(){
        getDiagnosticManager().clearDiagnosedSessions();
        futureOutcomesBySessionId.clear();
    }

    @AfterGroups("engine")
    public void afterClass() {
        if(sessionEngine != null && !sessionEngine.isClosed()){
            sessionEngine.close();
        }
        if(workerQueue != null){
            workerQueue.close();
        }
        if(appContext != null){
            SpringApplication.exit(appContext);
        }
    }

    public static ConfigurableApplicationContext getAppContext() {
        return appContext;
    }

    public static SessionEngine getSessionEngine() {
        return sessionEngine;
    }

    public static DiagnosticManager getDiagnosticManager() {
        return diagnosticManager;
    }

    public static WorkerQueue getWorkerQueue() {
        return workerQueue;
    }

    public static ExpressionResult executeOperation(Operation operation) throws AssertionError {
        Session session = submitOperation(operation);
        ExpressionResult resolvedOutcome = awaitOperation(session);

        logger.info("Operation " + operation.getOperationName() + " Completed with result " + resolvedOutcome.getOutcome());
        logger.info("Result Message: " + resolvedOutcome.getMessage());
        return resolvedOutcome;
    }

    public static Session submitOperation(Operation operation){
        try {
            Session session = getSessionEngine().initializeSession(operation);
            getDiagnosticManager().registerSession(session.getSessionShellId());
            Future<ExpressionResult> operationResult = getWorkerQueue().submit(() -> getSessionEngine().executeOperation(session, operation));
            futureOutcomesBySessionId.put(session.getSessionShellId(), operationResult);
            return session;
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            throw new NullPointerException(e.getMessage()); //should not be reached, as Assert.fail() will throw an assertion error
        }
    }

    public static ExpressionResult awaitOperation(Session session){
        return awaitOperation(session, futureOutcomesBySessionId.get(session.getSessionShellId()));
    }

    private static ExpressionResult awaitOperation(Session session, Future<ExpressionResult> runningOperation){
        ExpressionResult resolvedOutcome = null;
        try {
            resolvedOutcome = runningOperation.get();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            try {
                futureOutcomesBySessionId.remove(session.getSessionShellId());
                getSessionEngine().finalizeSession(session);
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
