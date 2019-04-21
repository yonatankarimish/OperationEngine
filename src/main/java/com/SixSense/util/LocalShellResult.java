package com.SixSense.util;

import java.util.List;

public class LocalShellResult {
    private int exitCode;
    private List<String> output;
    private List<String> errors;

    public LocalShellResult(int exitCode, List<String> output, List<String> errors) {
        this.exitCode = exitCode;
        this.output = output;
        this.errors = errors;
    }

    public boolean succeeded(){
        return this.exitCode == 0;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public List<String> getOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "LocalShellResult{" +
                "exitCode=" + exitCode +
                ", output='" + output + '\'' +
                ", errors='" + errors + '\'' +
                '}';
    }
}
