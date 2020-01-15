package com.SixSense.data.devices;

import com.SixSense.data.commands.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RawExecutionConfig {
    private Operation operation;
    private List<Device> devices;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for complete constructors - where all arguments are known */
    public RawExecutionConfig(){
        this.operation = new Operation();
        this.devices = new ArrayList<>();
    }

    public RawExecutionConfig(Operation operation, List<Device> devices) {
        this.operation = operation;
        this.devices = devices;
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
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public RawExecutionConfig withDevices(List<Device> devices) {
        this.devices = devices;
        return this;
    }

    //Returns a new instance of the same credentials in its pristine state. That is - as if the new state was never executed
    public RawExecutionConfig deepClone(){
        List<Device> clonedDevices = this.devices.stream().map(Device::deepClone).collect(Collectors.toList());

        return new RawExecutionConfig()
            .withOperation(operation.deepClone())
            .withDevices(clonedDevices);
    }

    @Override
    public String toString() {
        return "RawExecutionConfig{" +
            "operation=" + operation +
            ", devices=" + devices +
            '}';
    }
}
