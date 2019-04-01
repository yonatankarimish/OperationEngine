package com.SixSense.data.retention;

public class VariableRetention {
    private ResultRetention resultRetention;
    private String name;
    private boolean overwriteParent;

    public VariableRetention() {
        this.resultRetention = ResultRetention.None;
        this.name = "";
        this.overwriteParent = false;
    }

    public VariableRetention(ResultRetention resultRetention, String name, boolean overwriteParent) {
        this.resultRetention = resultRetention;
        this.name = name;
        this.overwriteParent = overwriteParent;
    }

    public ResultRetention getResultRetention() {
        return resultRetention;
    }

    public void setResultRetention(ResultRetention resultRetention) {
        this.resultRetention = resultRetention;
    }

    public VariableRetention withResultRetention(ResultRetention resultRetention) {
        this.resultRetention = resultRetention;
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

    @Override
    public String toString() {
        return "VariableRetention{" +
                "resultRetention=" + resultRetention +
                ", name='" + name + '\'' +
                ", overwriteParent=" + overwriteParent +
                '}';
    }
}
