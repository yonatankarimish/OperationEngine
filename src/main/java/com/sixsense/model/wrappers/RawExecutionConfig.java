package com.sixsense.model.wrappers;

import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.retention.OperationResult;

import java.util.*;

public class RawExecutionConfig implements IDeepCloneable<RawExecutionConfig> {
    private AdministrativeConfig administrativeConfig;
    private Operation operation;
    private Map<String, OperationResult> results;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for complete constructors - where all arguments are known */
    public RawExecutionConfig(){
        this.administrativeConfig = new AdministrativeConfig();
        this.operation = new Operation();
        this.results = new LinkedHashMap<>();
    }

    public RawExecutionConfig(AdministrativeConfig administrativeConfig, Operation operation) {
        this.administrativeConfig = administrativeConfig;
        this.operation = operation;
        this.results = new LinkedHashMap<>();
    }

    public AdministrativeConfig getAdministrativeConfig() {
        return administrativeConfig;
    }

    public void setAdministrativeConfig(AdministrativeConfig administrativeConfig) {
        this.administrativeConfig = administrativeConfig;
    }

    public RawExecutionConfig withAdministrativeConfig(AdministrativeConfig administrativeConfig) {
        this.administrativeConfig = administrativeConfig;
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

    //Returns a new instance of the same credentials in its pristine state. That is - as if the new state was never executed
    @Override
    public RawExecutionConfig deepClone(){
        return new RawExecutionConfig()
            .withAdministrativeConfig(administrativeConfig.deepClone())
            .withOperation(operation.deepClone());
            //no need to clone results, as they should never pass to a clone
    }

    @Override
    public boolean equals(Object other) {
        return this == other; // Raw configs never equal each other; They are different configurations received in different points in time
    }

    @Override
    public int hashCode() {
        return Objects.hash(administrativeConfig, operation, results);
    }

    @Override
    public String toString() {
        return "RawExecutionConfig{" +
            "administrativeConfig=" + administrativeConfig +
            ", operation=" + operation +
            ", results=" + results +
            '}';
    }
}
