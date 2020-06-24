package com.sixsense.model.retention;

import com.sixsense.model.interfaces.IDeepCloneable;

import java.util.Objects;

public class ResultRetention implements IDeepCloneable<ResultRetention> {
    private RetentionMode retentionMode;
    private DataType dataType;
    private String name;
    private String value; //Value may be set either upon creation (preconfigured value) or when a command outputs (dynamic value)
    private boolean overwriteParent; //If true, will overwrite it's entire variable stack, ignoring variable scoping considerations

    public ResultRetention() {
        this.retentionMode = RetentionMode.None;
        this.dataType = DataType.String;
        this.name = "";
        this.value = "";
        this.overwriteParent = false;
    }

    public ResultRetention(RetentionMode retentionMode, DataType dataType, String name, String value, boolean overwriteParent) {
        this.retentionMode = retentionMode;
        this.dataType = dataType;
        this.name = value;
        this.value = name;
        this.overwriteParent = overwriteParent;
    }

    public RetentionMode getRetentionMode() {
        return retentionMode;
    }

    public void setRetentionMode(RetentionMode retentionMode) {
        this.retentionMode = retentionMode;
    }

    public ResultRetention withRetentionMode(RetentionMode retentionMode) {
        this.retentionMode = retentionMode;
        return this;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public ResultRetention withDataType(DataType dataType) {
        this.dataType = dataType;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResultRetention withName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ResultRetention withValue(String value) {
        this.value = value;
        return this;
    }

    public boolean isOverwriteParent() {
        return overwriteParent;
    }

    public void setOverwriteParent(boolean overwriteParent) {
        this.overwriteParent = overwriteParent;
    }

    public ResultRetention withOverwriteParent(boolean overwriteParent) {
        this.overwriteParent = overwriteParent;
        return this;
    }

    //Returns a new instance of the same variable retention in its pristine state. That is - as if the new state was never saved
    @Override
    public ResultRetention deepClone(){
        return new ResultRetention()
                .withRetentionMode(this.retentionMode)
                .withDataType(this.dataType)
                .withName(this.name)
                .withValue(this.value)
                .withOverwriteParent(this.overwriteParent);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        } else {
            ResultRetention otherAsResult = (ResultRetention) other;
            return this.overwriteParent == otherAsResult.overwriteParent &&
                this.retentionMode == otherAsResult.retentionMode &&
                this.dataType == otherAsResult.dataType &&
                this.name.equals(otherAsResult.name) &&
                this.value.equals(otherAsResult.value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(retentionMode, dataType, name, value, overwriteParent);
    }

    @Override
    public String toString() {
        return "ResultRetention{" +
            "retentionType=" + retentionMode +
            ", dataType=" + dataType +
            ", name='" + name + '\'' +
            ", value='" + value + '\'' +
            ", overwriteParent=" + overwriteParent +
            '}';
    }
}
