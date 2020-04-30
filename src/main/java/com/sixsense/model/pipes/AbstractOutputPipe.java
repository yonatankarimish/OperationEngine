package com.sixsense.model.pipes;

import com.sixsense.io.Session;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@class")
@JsonSubTypes({
    @JsonSubTypes.Type(value=ClearingPipe.class, name = "ClearingPipe"),
    @JsonSubTypes.Type(value=DrainingPipe.class, name = "DrainingPipe"),
    @JsonSubTypes.Type(value=FirstLinePipe.class, name = "FirstLinePipe"),
    @JsonSubTypes.Type(value=LastLinePipe.class, name = "LastLinePipe"),
    @JsonSubTypes.Type(value=WhitespacePipe.class, name = "WhitespacePipe")
})
public abstract class AbstractOutputPipe {
    private String pipeType;

    public AbstractOutputPipe(){
        this.pipeType = this.getClass().getName();
    }

    public String pipe(Session session, String output){
        //While this default implementation exists, sometimes it makes sense to override it (e.g. First and Last output pipes)
        List<String> pipedOutputWrapper = pipe(session, Collections.singletonList(output));
        return pipedOutputWrapper.get(0);
    }
    public abstract List<String> pipe(Session session, List<String> output);
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        return Objects.hash(pipeType);
    }
}
