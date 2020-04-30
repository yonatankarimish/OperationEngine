package com.sixsense.model.devices;

import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.interfaces.IEquatable;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class Device implements IDeepCloneable<Device>, IEquatable<Device> {
    private final UUID uuid;
    private VendorProductVersion vpv;
    private Credentials credentials; //In a sense, just another set of dynamic fields; Acts as a convenience parameter
    private Map<String, String> dynamicFields;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for conditions, results and channel names */
    public Device() {
        this.uuid = UUID.randomUUID();
        this.vpv = new VendorProductVersion();
        this.credentials = new Credentials();
        this.dynamicFields = new HashMap<>();
    }

    public Device(VendorProductVersion vpv, Credentials credentials, Map<String, String> dynamicFields) {
        this.uuid = UUID.randomUUID();
        this.vpv = vpv;
        this.credentials = credentials;
        this.dynamicFields = dynamicFields;
    }

    @JsonIgnore
    public String getUUID() {
        return uuid.toString();
    }

    @JsonIgnore
    public String getShortUUID() {
        return uuid.toString().substring(0,8);
    }

    public VendorProductVersion getVpv() {
        return vpv;
    }

    public void setVpv(VendorProductVersion vpv) {
        this.vpv = vpv;
    }

    public Device withVpv(VendorProductVersion vpv) {
        this.vpv = vpv;
        return this;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Device withCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public Map<String, String> getDynamicFields() {
        return Collections.unmodifiableMap(this.dynamicFields);
    }

    public Device addDynamicField(String key, String value) {
        this.dynamicFields.put(key, value);
        return this;
    }

    public Device addDynamicFields(Map<String, String> dynamicFields) {
        this.dynamicFields.putAll(dynamicFields);
        return this;
    }

    //Returns a new instance of the same device in its pristine state. That is - as if the new state was never executed
    @Override
    public Device deepClone(){
        return new Device()
                .withVpv(this.vpv.deepClone())
                .withCredentials(this.credentials.deepClone())
                .addDynamicFields(this.dynamicFields);
    }

    @Override
    public boolean weakEquals(Device other) {
        return this.vpv.equals(other.vpv) &&
            this.credentials.getHost().equals(other.credentials.getHost()) &&
            this.credentials.getPort() == other.credentials.getPort();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || this.getClass() != other.getClass()) {
            return false;
        } else {
            return this.equals((Device) other);
        }
    }

    public boolean equals(Device other) {
        return this.vpv.equals(other.vpv) && this.credentials.equals(other.credentials);
    }

    @Override
    public boolean strongEquals(Device other) {
        return this.equals(other) && this.dynamicFields.equals(other.dynamicFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vpv, credentials);
    }

    @Override
    public String toString() {
        return "Device{" +
            "uuid=" + uuid +
            ", vpv=" + vpv +
            ", credentials=" + credentials +
            ", dynamicFields=" + dynamicFields +
            '}';
    }
}
