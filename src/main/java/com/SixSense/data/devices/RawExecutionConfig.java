package com.SixSense.data.devices;

import com.SixSense.data.commands.Operation;
import com.SixSense.data.retention.OperationResult;

import java.util.*;
import java.util.stream.Collectors;

public class RawExecutionConfig {
    private Operation operation;
    private List<Device> devices;
    private Map<String, OperationResult> results;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for complete constructors - where all arguments are known */
    public RawExecutionConfig(){
        this.operation = new Operation();
        this.devices = new ArrayList<>();
        this.results = new LinkedHashMap<>();
    }

    public RawExecutionConfig(Operation operation, List<Device> devices) {
        this.operation = operation;
        this.devices = devices;
        this.results = new LinkedHashMap<>();
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public RawExecutionConfig withOperation(Operation operation) {
        this.operation = operation;
        return this;
    }

    public List<Device> getDevices() {
        return Collections.unmodifiableList(devices);
    }

    public RawExecutionConfig addDevice(Device device) {
        this.devices.add(device);
        return this;
    }

    public RawExecutionConfig addDevices(List<Device> devices) {
        this.devices.addAll(devices);
        return this;
    }

    public Map<String, OperationResult> getResults() {
        return Collections.unmodifiableMap(results);
    }

    public RawExecutionConfig addResult(String deviceId, OperationResult result) {
        this.results.put(deviceId, result);
        return this;
    }

    public RawExecutionConfig addResults(Map<String, OperationResult> results) {
        this.results.putAll(results);
        return this;
    }

    //Returns a new instance of the same credentials in its pristine state. That is - as if the new state was never executed
    public RawExecutionConfig deepClone(){
        List<Device> clonedDevices = this.devices.stream().map(Device::deepClone).collect(Collectors.toList());

        return new RawExecutionConfig()
            .withOperation(operation.deepClone())
            .addDevices(clonedDevices);
    }

    @Override
    public String toString() {
        return "RawExecutionConfig{" +
            "operation=" + operation +
            ", devices=" + devices +
            ", results=" + results +
            '}';
    }
}
