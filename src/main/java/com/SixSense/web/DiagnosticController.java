package com.SixSense.web;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.devices.Device;
import com.SixSense.data.devices.VendorProductVersion;
import com.SixSense.engine.SessionEngine;
import com.SixSense.util.DynamicFieldGlossary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticController {
    private static final Logger logger = LogManager.getLogger(DiagnosticController.class);
    private final SessionEngine sessionEngine;

    @Autowired
    public DiagnosticController(SessionEngine sessionEngine) {
        this.sessionEngine = sessionEngine;
    }

    @GetMapping("/config")
    public Map<String, String> getEngineConfig() {
        return SessionEngine.getSessionProperties();
    }

    @PostMapping("/config")
    public Map<String, String> updateEngineConfig(@RequestBody Map<String, String> updatedConfig) throws IOException {
        return SessionEngine.addSessionProperties(updatedConfig);
    }

    /*@GetMapping("/status")
    public Map<String, String> getEngineStatus() {

    }*/

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

    /*@GetMapping("/logs/{sessionId}")
    public Map<String, String> getSessionLogs() {

    }*/
}
