package com.SixSense.api.http.controllers;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.threading.MonitoredThreadState;
import com.SixSense.engine.SessionEngine;
import com.SixSense.threading.ThreadingManager;
import com.SixSense.util.DynamicFieldGlossary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticController extends DebuggableHttpController {
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
    public Map<String, MonitoredThreadState> getEngineStatus() {
        return threadingManager.getEngineThreadStatus();
    }

    @GetMapping("/operations")
    public Set<String> getRunningOperations() {
        return sessionEngine.getOperationsToSessions().keySet();
    }

    @GetMapping("/devices/running")
    public Map<String, String> getRunningDevices() {
        Map<String, Operation> runningOperations = sessionEngine.getRunningOperations();
        Map<String, String> runningDevices = new HashMap<>();

        for(String operationId : runningOperations.keySet()){
            String deviceHost = runningOperations.get(operationId).getDynamicFields().get(DynamicFieldGlossary.device_host);
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
    }
}
