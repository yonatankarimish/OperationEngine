package com.SixSense.pipes;

import java.util.List;
import java.util.Objects;

public abstract class AbstractOutputPipe {
    private String pipeType;

    public AbstractOutputPipe(){
        this.pipeType = this.getClass().getName();
    }

    public abstract List<String> pipe(List<String> output);
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        return Objects.hash(pipeType);
    }
}
