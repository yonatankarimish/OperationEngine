package com.sixsense.api.http.controllers;

import com.sixsense.api.ApiDebuggingAware;
import com.sixsense.mocks.OperationMocks;
import com.sixsense.model.commands.Command;
import com.sixsense.model.commands.ICommand;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.commands.ParallelWorkflow;
import com.sixsense.model.devices.Credentials;
import com.sixsense.model.logic.*;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.model.retention.RetentionMode;
import com.sixsense.model.threading.MonitoredThreadState;
import com.sixsense.services.SessionEngine;
import com.sixsense.threading.ThreadingManager;
import com.sixsense.utillity.CommandUtils;
import com.sixsense.utillity.FieldGlossary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticController extends ApiDebuggingAware {
    private static final Logger logger = LogManager.getLogger(DiagnosticController.class);
    private final SessionEngine sessionEngine;
    private final ThreadingManager threadingManager;
    private final CachingConnectionFactory amqpConnectionFactory;

    private static final double toSecondCoefficient = Math.pow(10, -9);

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
    }

    @GetMapping("/stressTest/remote/{operationsPerDevice}")
    public void stressTestRemote(@PathVariable int operationsPerDevice){
        int octet3limit = 5;
        int octet4limit = 254;

        List<Credentials> credentials = new ArrayList<>();
        for(int octet3=1; octet3<=octet3limit; octet3++){
            for(int octet4=1; octet4<=octet4limit; octet4++){
                String host = "172.3." + octet3 + "." + octet4;
                credentials.add(
                    new Credentials()
                    .withHost(host)
                    .withUsername("root")
                    .withPassword("VMware1!")
                );
            }
        }

        ParallelWorkflow workflow = CommandUtils.composeWorkflow(OperationMocks.tinycoreLabEcho(credentials));
        String operationName = workflow.getParallelOperations().get(0).getOperationName(); //We can get the first operation, since only one credential set was passed
        int devicesCount = workflow.getParallelOperations().size();
        int totalOperations = operationsPerDevice * devicesCount;

        List<Double> sessionDurations = Collections.synchronizedList(new ArrayList<>());
        List<OperationResult> operationResults = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch counter = new CountDownLatch(totalOperations);
        Instant batchStart = Instant.now();

        logger.info(" "); //new line without breaking log format
        logger.info("Starting benchmark tests for " + devicesCount + " devices, running " + operationsPerDevice + " operations each (total: " + totalOperations + " operations)");
        logger.info("Operation name: " + operationName);

        int ordinal = 1;
        for(int i = 1; i<=operationsPerDevice; i++) {
            for(Operation operation : workflow.getParallelOperations()) {
                AtomicInteger ordinalRef = new AtomicInteger(ordinal++);

                threadingManager.submit(() -> {
                    OperationResult operationResult = null;
                    Instant sessionStart = Instant.now();
                    logger.debug("Before starting session #" + ordinalRef.get());
                    try {
                        operationResult = sessionEngine.executeOperation(operation.deepClone());
                    } catch (Exception e) {
                        logger.info("Failed to execute session #" + ordinalRef.get() + ". Caused by: ", e);
                    }
                    logger.debug("After closing session #" + ordinalRef.get());

                    Instant sessionEnd = Instant.now();
                    Duration sessionDuration = Duration.between(sessionStart, sessionEnd);
                    sessionDurations.add(sessionDuration.getSeconds() + toSecondCoefficient * sessionDuration.getNano());

                    counter.countDown();
                    operationResults.add(operationResult);
                });
            }
        }

        try {
            counter.await();
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for countdown latch. Caused by: ", e);
        }

        logger.info("Finished benchmark tests for current batch");
        Instant batchEnd = Instant.now();
        Duration batchDuration = Duration.between(batchStart, batchEnd);

        printStats(sessionDurations, operationResults, batchDuration, totalOperations);
    }

    @GetMapping("/stressTest/local/{operationCount}")
    public void stressTestLocal(@PathVariable int operationCount){
        ICommand simpleCommand = new Command()
            .withChannel(ChannelType.LOCAL)
            .withCommandText("echo lorem ipsum")
            .withMinimalSecondsToResponse(3)
            .withSecondsToTimeout(30)
            .withExpectedOutcome(
                new LogicalExpression<ExpectedOutcome>()
                    .addResolvable(
                        new ExpectedOutcome()
                            .withBinaryRelation(BinaryRelation.CONTAINS)
                            .withExpectedValue("lorem ipsum")
                    )
            )
            .withSaveTo(
                new ResultRetention()
                .withName("var.lorem.ipsum")
                .withRetentionMode(RetentionMode.DatabaseEventual)
            );

        Operation simpleOperation = new Operation()
            .withOperationName("Echo placeholder text")
            .withExecutionBlock(simpleCommand)
            .addChannel(ChannelType.LOCAL);

        stressTest(simpleOperation, operationCount);
    }

    private void stressTest(Operation operation, int operationCount){
        List<Double> sessionDurations = Collections.synchronizedList(new ArrayList<>());
        List<OperationResult> operationResults = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch counter = new CountDownLatch(operationCount);
        Instant batchStart = Instant.now();

        logger.info(" "); //new line without breaking log format
        logger.info("Starting benchmark tests for " + operationCount + " operations");
        logger.info("Operation name: " + operation.getOperationName());
        for(int i = 1; i<=operationCount; i++) {
            AtomicInteger ordinal = new AtomicInteger(i);
            Operation clonedOperation = operation.deepClone();

            threadingManager.submit(() -> {
                OperationResult operationResult = null;
                Instant sessionStart = Instant.now();
                logger.debug("Before starting session #" + ordinal.get());
                try {
                    operationResult = sessionEngine.executeOperation(clonedOperation);
                } catch (Exception e) {
                    logger.info("Failed to execute session #" + ordinal.get() + ". Caused by: ", e);
                }
                logger.debug("After closing session #" + ordinal.get());

                Instant sessionEnd = Instant.now();
                Duration sessionDuration = Duration.between(sessionStart, sessionEnd);
                sessionDurations.add(sessionDuration.getSeconds() + toSecondCoefficient * sessionDuration.getNano());

                counter.countDown();
                operationResults.add(operationResult);
            });
        }

        try {
            counter.await();
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for countdown latch. Caused by: ", e);
        }

        logger.info("Finished benchmark tests for current batch");
        Instant batchEnd = Instant.now();
        Duration batchDuration = Duration.between(batchStart, batchEnd);

        printStats(sessionDurations, operationResults, batchDuration, operationCount);
    }


    private void printStats(List<Double> sessionDurations, List<OperationResult> operationResults, Duration batchDuration, int operationCount){
        Collections.sort(sessionDurations);
        double medianTime = sessionDurations.get(operationCount / 2);
        double averageTime = sessionDurations.stream().reduce((a1, a2) -> a1 + a2).get() / operationCount;
        double batchTime =  batchDuration.getSeconds() + toSecondCoefficient * batchDuration.getNano();

        logger.info(" "); //new line without breaking log format
        logger.info("Running " + operationCount + " operations took " + batchTime + " seconds");
        logger.info("The median running time for all operations was " + medianTime + " seconds");
        logger.info("The average running time of an operations was " + averageTime + " seconds");

        List<ResultStatus> results = operationResults.stream().map(result -> result.getExpressionResult().getOutcome()).collect(Collectors.toList());
        List<String> errors = operationResults.stream().map(result -> result.getExpressionResult().getMessage()).collect(Collectors.toList());

        long successes = results.stream().filter(status -> status.equals(ResultStatus.SUCCESS)).count();
        double successPercentage  = Math.floor((double)successes / (double)operationCount * 10000) / 100;

        Map<String, Integer> errorFrequencies = new LinkedHashMap<>();
        errors.forEach(error -> {
            if(!error.isBlank()) {
                errorFrequencies.putIfAbsent(error, 0);
                int frequency = errorFrequencies.get(error);
                errorFrequencies.put(error, frequency + 1);
            }
        });

        logger.info(successes + " operations were successful (" + successPercentage + "%)");
        if(errorFrequencies.isEmpty()){
            logger.info("No errors encountered, therefore there is no need for an error frequency table");
        }else {
            logger.info("Error frequency by type:");
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(errorFrequencies.entrySet());
            entries.sort(Map.Entry.comparingByValue());
            Collections.reverse(entries);
            entries.forEach(frequency -> logger.info(frequency.getKey() + " => " + frequency.getValue()));
        }
    }
}
