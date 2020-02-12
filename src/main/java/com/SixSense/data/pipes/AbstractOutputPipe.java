package com.SixSense.data.pipes;

import com.SixSense.io.Session;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Objects;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@class")
@JsonSubTypes({
    @JsonSubTypes.Type(value=ClearingPipe.class, name = "ClearingPipe"),
    @JsonSubTypes.Type(value=DrainingPipe.class, name = "DrainingPipe"),
    @JsonSubTypes.Type(value=FirstLinePipe.class, name = "FirstLinePipe"),
    @JsonSubTypes.Type(value=LastLinePipe.class, name = "LastLinePipe")
})
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
