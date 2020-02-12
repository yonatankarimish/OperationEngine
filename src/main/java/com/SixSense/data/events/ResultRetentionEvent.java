package com.SixSense.data.events;

import com.SixSense.data.retention.ResultRetention;
import com.SixSense.io.Session;

public class ResultRetentionEvent extends AbstractEngineEvent{
    private ResultRetention resultRetention;

    public ResultRetentionEvent(Session session, ResultRetention resultRetention) {
        super(EngineEventType.ResultRetention, session);
        this.resultRetention = resultRetention;
    }

    public ResultRetention getResultRetention() {
        return resultRetention;
    }
}
