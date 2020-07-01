package com.sixsense.model.wrappers;

import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.retention.OperationResult;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class RawTerminationConfig implements IDeepCloneable<RawTerminationConfig> {
    private Set<String> operationIds;
    Map<String, OperationResult> results;

    private Instant startTime;
    private Instant endTime;

    private RawTerminationConfig(){
        this.operationIds = new HashSet<>();
        this.results = new HashMap<>();
        this.startTime = Instant.now();
        this.endTime = Instant.now().plusMillis(1); //to prevent end time matching start time
    }

    public RawTerminationConfig(Set<String> operationIds) {
        this.operationIds = operationIds;
        this.results = new HashMap<>();
        this.startTime = Instant.now();
        this.endTime = Instant.now().plusMillis(1); //to prevent end time matching start time
    }

    public Set<String> getOperationIds() {
        return Collections.unmodifiableSet(operationIds);
    }

    public RawTerminationConfig addOperationId(String operationId) {
        this.operationIds.add(operationId);
        return this;
    }

    public RawTerminationConfig addOperationIds(Set<String> operationIds) {
        this.operationIds.addAll(operationIds);
        return this;
    }

    public Map<String, OperationResult> getResults() {
        return Collections.unmodifiableMap(results);
    }

    public RawTerminationConfig addResult(String deviceId, OperationResult result) {
        this.results.put(deviceId, result);
        return this;
    }

    public RawTerminationConfig addResults(Map<String, OperationResult> results) {
        this.results.putAll(results);
        return this;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public RawTerminationConfig withStartTime(Instant startTime) {
        this.startTime = startTime;
        return this;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public RawTerminationConfig withEndTime(Instant endTime) {
        this.endTime = endTime;
        return this;
    }

    //Returns a new instance of the same config in its pristine state. That is - as if the new state was never executed
    @Override
    public RawTerminationConfig deepClone(){
        Set<String> clonedOperationIds = new HashSet<>(this.operationIds);

        return new RawTerminationConfig().addOperationIds(clonedOperationIds);
        //no need to clone results, as they should never pass to a clone
    }

    @Override
    public boolean equals(Object other) {
        return this == other; // Raw configs never equal each other; They are different configurations received in different points in time
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationIds, startTime, endTime);
    }

    @Override
    public String toString() {
        return "RawTerminationConfig{" +
            "operationIds=" + operationIds +
            ", results=" + results +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            '}';
    }
}
