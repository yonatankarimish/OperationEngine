package com.sixsense.model.wrappers;

import com.sixsense.model.devices.Device;
import com.sixsense.model.interfaces.IDeepCloneable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AdministrativeConfig implements IDeepCloneable<AdministrativeConfig> {
    private Instant startTime;
    private Instant endTime;
    private List<Device> devices;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for complete constructors - where all arguments are known */
    public AdministrativeConfig() {
        this.startTime = Instant.now();
        this.endTime = Instant.now().plusMillis(1); //to prevent end time matching start time
        this.devices = new ArrayList<>();
    }

    public AdministrativeConfig(Instant startTime, Instant endTime, List<Device> devices) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.devices = devices;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public AdministrativeConfig withStartTime(Instant startTime) {
        this.startTime = startTime;
        return this;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public AdministrativeConfig withEndTime(Instant endTime) {
        this.endTime = endTime;
        return this;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public AdministrativeConfig addDevice(Device device){
        this.devices.add(device);
        return this;
    }

    public AdministrativeConfig addDevices(List<Device> devices){
        this.devices.addAll(devices);
        return this;
    }

    @Override
    public AdministrativeConfig deepClone(){
        List<Device> clonedDevices = this.devices.stream().map(Device::deepClone).collect(Collectors.toList());

        return new AdministrativeConfig()
            .withStartTime(this.startTime)
            .withEndTime(this.endTime)
            .addDevices(clonedDevices);
    }

    @Override
    public boolean equals(Object other) {
        return this == other; // Administrative configs never equal each other; They are different configurations received in different points in time
    }

    @Override
    public int hashCode() {
        return Objects.hash(devices, startTime, endTime);
    }

    @Override
    public String toString() {
        return "AdministrativeConfig{" +
            "startTime=" + startTime +
            ", endTime=" + endTime +
            ", devices=" + devices +
            '}';
    }
}
