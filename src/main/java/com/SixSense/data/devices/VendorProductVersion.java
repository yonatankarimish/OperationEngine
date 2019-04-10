package com.SixSense.data.devices;

public class VendorProductVersion {
    private String vendor;
    private String product;
    private String version;

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

    public String getName(){
        return vendor + " " + product + " " + version;
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
