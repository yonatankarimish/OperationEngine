package com.SixSense.data.devices;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Device {
    private VendorProductVersion vpv;
    private Map<String, String> dynamicFields;

    public Device() {
        this.vpv = new VendorProductVersion();
        this.dynamicFields = new HashMap<>();
    }

    public Device(VendorProductVersion vpv, Map<String, String> dynamicFields) {
        this.vpv = vpv;
        this.dynamicFields = dynamicFields;
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
    public Device deepClone(){
        return new Device()
                .withVpv(this.vpv.deepClone())
                .addDynamicFields(this.dynamicFields);
    }

    @Override
    public String toString() {
        return "Device{" +
                "vpv=" + vpv +
                ", dynamicFields=" + dynamicFields +
                '}';
    }
}
