package com.sixsense.model.wrappers;

import com.sixsense.model.devices.Device;
import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.retention.OperationResult;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class RawExecutionConfig implements IDeepCloneable<RawExecutionConfig> {
    private List<Device> devices;
    private Operation operation;
    private Map<String, OperationResult> results;

    private Instant startTime;
    private Instant endTime;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for complete constructors - where all arguments are known */
    public RawExecutionConfig() {
        this.devices = new ArrayList<>();
        this.operation = new Operation();
        this.results = new LinkedHashMap<>();
        this.startTime = Instant.now();
        this.endTime = Instant.now().plusMillis(1); //to prevent end time matching start time
    }

    public RawExecutionConfig(List<Device> devices, Operation operation) {
        this.operation = operation;
        this.devices = devices;
        this.results = new LinkedHashMap<>();
        this.startTime = Instant.now();
        this.endTime = Instant.now().plusMillis(1); //to prevent end time matching start time
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

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public RawExecutionConfig withStartTime(Instant startTime) {
        this.startTime = startTime;
        return this;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public RawExecutionConfig withEndTime(Instant endTime) {
        this.endTime = endTime;
        return this;
    }

    //Returns a new instance of the same config in its pristine state. That is - as if the new state was never executed
    @Override
    public RawExecutionConfig deepClone() {
        List<Device> clonedDevices = this.devices.stream().map(Device::deepClone).collect(Collectors.toList());

        return new RawExecutionConfig()
            .addDevices(clonedDevices)
            .withOperation(operation.deepClone())
            .withStartTime(this.startTime)
            .withEndTime(this.endTime);
        //no need to clone results, as they should never pass to a clone
    }

    @Override
    public boolean equals(Object other) {
        return this == other; // Raw configs never equal each other; They are different configurations received in different points in time
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation.getUUID(), startTime, endTime);
    }

    @Override
    public String toString() {
        return "RawExecutionConfig{" +
            "devices=" + devices +
            ", operation=" + operation +
            ", results=" + results +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            '}';
    }
}
