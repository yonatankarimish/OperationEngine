package com.SixSense.data.retention;

public class VariableRetention{
    private RetentionType retentionType;
    private String name;
    private String value; //Value may be set either upon creation (preconfigured value) or when a command outputs (dynamic value)
    private boolean overwriteParent; //If true, will overwrite it's entire variable stack, ignoring variable scoping considerations

    public VariableRetention() {
        this.retentionType = RetentionType.None;
        this.name = "";
        this.value = "";
        this.overwriteParent = false;
    }

    public VariableRetention(RetentionType retentionType, String name, String value, boolean overwriteParent) {
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

    public VariableRetention withRetentionType(RetentionType retentionType) {
        this.retentionType = retentionType;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VariableRetention withName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public VariableRetention withValue(String value) {
        this.value = value;
        return this;
    }

    public boolean isOverwriteParent() {
        return overwriteParent;
    }

    public void setOverwriteParent(boolean overwriteParent) {
        this.overwriteParent = overwriteParent;
    }

    public VariableRetention withOverwriteParent(boolean overwriteParent) {
        this.overwriteParent = overwriteParent;
        return this;
    }

    //Returns a new instance of the same variable retention in its pristine state. That is - as if the new state was never saved
    public VariableRetention deepClone(){
        return new VariableRetention()
                .withRetentionType(this.retentionType)
                .withName(this.name)
                .withValue(this.value)
                .withOverwriteParent(this.overwriteParent);
    }

    @Override
    public String toString() {
        return "VariableRetention{" +
                "retentionType=" + retentionType +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", overwriteParent=" + overwriteParent +
                '}';
    }
}
