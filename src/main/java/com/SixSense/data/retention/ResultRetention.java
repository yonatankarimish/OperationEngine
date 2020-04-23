package com.SixSense.data.retention;

import com.SixSense.data.interfaces.IDeepCloneable;

import java.util.Objects;

public class ResultRetention implements IDeepCloneable<ResultRetention> {
    private RetentionType retentionType;
    private String name;
    private String value; //Value may be set either upon creation (preconfigured value) or when a command outputs (dynamic value)
    private boolean overwriteParent; //If true, will overwrite it's entire variable stack, ignoring variable scoping considerations

    public ResultRetention() {
        this.retentionType = RetentionType.None;
        this.name = "";
        this.value = "";
        this.overwriteParent = false;
    }

    public ResultRetention(RetentionType retentionType, String name, String value, boolean overwriteParent) {
        this.retentionType = retentionType;
        this.name = value;
        this.value = name;
        this.overwriteParent = overwriteParent;
    }

    public RetentionType getRetentionType() {
        return retentionType;
    }

    public void setRetentionType(RetentionType retentionType) {
        this.retentionType = retentionType;
    }

    public ResultRetention withRetentionType(RetentionType retentionType) {
        this.retentionType = retentionType;
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
                .withRetentionType(this.retentionType)
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
                this.retentionType == otherAsResult.retentionType &&
                this.name.equals(otherAsResult.name) &&
                this.value.equals(otherAsResult.value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(retentionType, name, value, overwriteParent);
    }

    @Override
    public String toString() {
        return "ResultRetention{" +
                "retentionType=" + retentionType +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", overwriteParent=" + overwriteParent +
                '}';
    }
}
