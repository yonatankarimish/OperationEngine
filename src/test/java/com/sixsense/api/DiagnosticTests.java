package com.sixsense.api;

import com.sixsense.SixSenseBaseTest;
import com.sixsense.SixSenseBaseUtils;
import com.sixsense.api.http.controllers.DiagnosticController;
import com.sixsense.model.aspects.MethodInvocation;
import com.sixsense.model.commands.*;
import com.sixsense.model.events.EngineEventType;
import com.sixsense.model.logic.ChannelType;
import com.sixsense.model.threading.MonitoredThreadState;
import com.sixsense.services.SessionEngine;
import com.sixsense.utillity.ThreadingUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

//It might be worth to add test cases for the cartesian product of chaining any two ICommands
@Test(groups = {"api"})
public class DiagnosticTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(DiagnosticTests.class);
    private static DiagnosticController diagnosticController;

    @BeforeClass
    private void initDiagnosticController(){
        diagnosticController = (DiagnosticController)SixSenseBaseUtils.getAppContext().getBean("diagnosticController");
        diagnosticController.activateDebugMode();
    }

    @AfterMethod
    private void resetDiagnosticController(){
        diagnosticController.clearMethodInvocations();
    }

    public void testGetEngineConfig(){
        //These two are (as of writing the test) obtained from the exact same point in code. This is more of an example of class usage than a truly meaningful test
        Map<String, String> configFromSessionEngine = SessionEngine.getSessionProperties();
        Map<String, String> configFromController = diagnosticController.getEngineConfig();
        Map<String, String> configFromMethodInvocation = null;

        MethodInvocation configInvocation = diagnosticController.getMethodInvocations().iterator().next();
        Assert.assertEquals(configInvocation.getMethodName(), "getEngineConfig");
        Assert.assertEquals(configInvocation.getInvocationArguments().length, 0);
        try {
            configFromMethodInvocation = (Map<String, String>)configInvocation.getReturnValue().get();
        } catch (Exception e) {
            Assert.fail("Failed to get configuration from method invocation. Caused by: ", e);
        }

        for(String key : configFromSessionEngine.keySet()){
            Assert.assertTrue(configFromController.containsKey(key));
            Assert.assertTrue(configFromMethodInvocation.containsKey(key));
            Assert.assertEquals(configFromSessionEngine.get(key), configFromController.get(key));
            Assert.assertEquals(configFromSessionEngine.get(key), configFromMethodInvocation.get(key));
        }
    }

    public void testUpdateEngineConfig(){
        Map<String, String> configFromSessionEngine = SessionEngine.getSessionProperties();
        Map<String, String> configurationUpdate = new HashMap<>();
        Random r = new Random();
        String updateKeyConstant = "test.config.update.key";
        for(int i=1; i<=3; i++) {
            configurationUpdate.put(updateKeyConstant+i, String.valueOf(r.nextInt()));
            Assert.assertFalse(configFromSessionEngine.containsKey(updateKeyConstant+i));
        }

        Map<String, String> updatedConfigFromController = diagnosticController.updateEngineConfig(configurationUpdate);
        Map<String, String> updatedConfigFromSessionEngine = SessionEngine.getSessionProperties();
        Map<String, String> updatedConfigFromMethodInvocation = null;

        MethodInvocation configInvocation = diagnosticController.getMethodInvocations().iterator().next();
        Assert.assertEquals(configInvocation.getMethodName(), "updateEngineConfig");
        Assert.assertEquals(configInvocation.getInvocationArguments().length, 1);
        Assert.assertEquals(configInvocation.getInvocationArguments()[0], configurationUpdate);
        try {
            updatedConfigFromMethodInvocation = (Map<String, String>)configInvocation.getReturnValue().get();
        } catch (Exception e) {
            Assert.fail("Failed to get updated configuration from method invocation. Caused by: ", e);
        }

        for(String key : configurationUpdate.keySet()){
            Assert.assertTrue(updatedConfigFromController.containsKey(key));
            Assert.assertTrue(updatedConfigFromSessionEngine.containsKey(key));
            Assert.assertTrue(updatedConfigFromMethodInvocation.containsKey(key));
            Assert.assertEquals(configurationUpdate.get(key), updatedConfigFromController.get(key));
            Assert.assertEquals(configurationUpdate.get(key), updatedConfigFromSessionEngine.get(key));
            Assert.assertEquals(configurationUpdate.get(key), updatedConfigFromMethodInvocation.get(key));
        }
    }

    public void testConfigurationIsUnmodifiable(){
        boolean successfullyModified = false;
        Map<String, String> config = diagnosticController.getEngineConfig();

        try{
            config.put("test.config.new.key", "this shouldn't work");
            successfullyModified = true;
        }catch (UnsupportedOperationException e){
            /*intentionally empty*/
        }

        try{
            config.remove("test.config.new.key", "this shouldn't work");
            successfullyModified = true;
        }catch (UnsupportedOperationException e){
            /*intentionally empty*/
        }

        try{
            config.clear();
            successfullyModified = true;
        }catch (UnsupportedOperationException e){
            /*intentionally empty*/
        }

        Assert.assertFalse(successfullyModified);
    }

    public void testEngineStatusIsNotEmpty(){
        CompletableFuture<Void> waitForMeToFinish = new CompletableFuture<>();
        AtomicReference<Long> threadIdRef = new AtomicReference<>(-1L);
        try {
            /*There is no guarantee that the engine will be running any thread at all
             *We pass in a supplier which waits for a Future to complete. Until then, at least one worker thread will be required to run it*/
            SixSenseBaseUtils.getThreadingManager().submit(() -> {
                ThreadingUtils.updateLifecyclePhase(EngineEventType.NotInSession);
                threadIdRef.set(Thread.currentThread().getId());
                try {
                    waitForMeToFinish.get();
                } catch (Exception e) {
                    /*Intentionally empty (who cares?)*/
                }
                return null;
            });


            Map<Long, MonitoredThreadState> engineStatus = diagnosticController.getEngineStatus();
            Assert.assertFalse(engineStatus.isEmpty());

            for(Map.Entry<Long, MonitoredThreadState> threadStatus : engineStatus.entrySet()){
                long currentThreadId = threadStatus.getKey();
                if(threadIdRef.get().equals(currentThreadId)){
                    MonitoredThreadState threadState = threadStatus.getValue();
                    Assert.assertEquals(threadState.getCurrentLifecyclePhase(), EngineEventType.NotInSession);
                    return;
                }
            }

            Assert.fail("Engine status does not show the submitted thread");

        }finally {
            waitForMeToFinish.complete(null);
        }
    }

    private Command simpleWaitingCommand(){
        return new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo lorem ipsum dolor sit amet")
            .withMinimalSecondsToResponse(15);
    }
}
