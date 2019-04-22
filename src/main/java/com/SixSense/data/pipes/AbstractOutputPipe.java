package com.SixSense.data.pipes;

import com.SixSense.io.Session;

import java.util.List;
import java.util.Objects;

public abstract class AbstractOutputPipe {
    private String pipeType;

    public AbstractOutputPipe(){
        this.pipeType = this.getClass().getName();
    }

    public abstract List<String> pipe(Session session, List<String> output);
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        return Objects.hash(pipeType);
    }
}
