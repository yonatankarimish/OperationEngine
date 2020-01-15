package com.SixSense.data.devices;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class VendorProductVersion {
    private String vendor;
    private String product;
    private String version;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for complete constructors - where all arguments are known */
    public VendorProductVersion() {
        this.vendor = "";
        this.product = "";
        this.version = "";
    }

    public VendorProductVersion(String vendor, String product, String version) {
        this.vendor = vendor;
        this.product = product;
        this.version = version;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public VendorProductVersion withVendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public VendorProductVersion withProduct(String product) {
        this.product = product;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public VendorProductVersion withVersion(String version) {
        this.version = version;
        return this;
    }

    @JsonIgnore
    public String getName(){
        return vendor + " " + product + " " + version;
    }

    //Returns a new instance of the same vpv in its pristine state. That is - as if the new state was never executed
    public VendorProductVersion deepClone(){
        return new VendorProductVersion()
                .withVendor(this.vendor)
                .withProduct(this.product)
                .withVersion(this.version);
    }

    @Override
    public String toString() {
        return "VendorProductVersion{" +
                "vendor='" + vendor + '\'' +
                ", product='" + product + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
