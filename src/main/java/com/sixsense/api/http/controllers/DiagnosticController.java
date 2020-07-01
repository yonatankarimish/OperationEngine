package com.sixsense.api.http.controllers;

import com.sixsense.api.ApiDebuggingAware;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.threading.MonitoredThreadState;
import com.sixsense.services.SessionEngine;
import com.sixsense.threading.ThreadingManager;
import com.sixsense.utillity.FieldGlossary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticController extends ApiDebuggingAware {
    private static final Logger logger = LogManager.getLogger(DiagnosticController.class);
    private final SessionEngine sessionEngine;
    private final ThreadingManager threadingManager;
    private final CachingConnectionFactory amqpConnectionFactory;

    @Autowired
    public DiagnosticController(SessionEngine sessionEngine, ThreadingManager threadingManager, CachingConnectionFactory amqpConnectionFactory) {
        super();
        this.sessionEngine = sessionEngine;
        this.threadingManager = threadingManager;
        this.amqpConnectionFactory = amqpConnectionFactory;
    }

    @GetMapping("/config")
    public Map<String, String> getEngineConfig() {
        return SessionEngine.getSessionProperties();
    }

    @PostMapping("/config")
    public Map<String, String> updateEngineConfig(@RequestBody Map<String, String> updatedConfig) {
        return SessionEngine.addSessionProperties(updatedConfig);
    }

    @GetMapping("/status")
    public Map<Long, MonitoredThreadState> getEngineStatus() {
        return threadingManager.getEngineThreadStatus();
    }

    @GetMapping("/operations")
    public Set<String> getRunningOperations() {
        return sessionEngine.getRunningOperations().keySet();
    }

    @GetMapping("/devices/running")
    public Map<String, String> getRunningDevices() {
        Map<String, Operation> runningOperations = sessionEngine.getRunningOperations();
        Map<String, String> runningDevices = new HashMap<>();

        for(String operationId : runningOperations.keySet()){
            String deviceHost = runningOperations.get(operationId).getDynamicFields().get(FieldGlossary.device_host);
            runningDevices.put(operationId, deviceHost);
        }

        return runningDevices;
    }

    @GetMapping("/amqp/cacheProperties")
    public Properties getAMQPCacheProperties(){
        return amqpConnectionFactory.getCacheProperties();
    }

    /*@GetMapping("/logs/{sessionId}")
    public Map<String, String> getSessionLogs() {

    }*/

    @GetMapping("/debugMethod")
    public void debugMethod(){
        //This method body acts as a pastebin for development and debugging purposes. Method body is subject to change without notice
        logger.info("Starting debug method now");

        /*Operation mock = OperationMocks.f5BigIpInventory(Collections.singletonList(
            new Credentials()
                .withHost("172.31.252.179")
                .withUsername("root")
                .withPassword("qwe123")
        )).getOperation();

        Block executionBlockA = (Block)mock.getExecutionBlock().deepClone();
        Block executionBlockB = (Block)mock.getExecutionBlock().deepClone();

        executionBlockA.setAlreadyExecuted(true);
        boolean weakEqual = executionBlockA.weakEquals(executionBlockB);
        boolean equal = executionBlockA.equals(executionBlockB);
        boolean strongEqual = executionBlockA.strongEquals(executionBlockB);

        logger.info("Finished debug method now");*/
    }
}
