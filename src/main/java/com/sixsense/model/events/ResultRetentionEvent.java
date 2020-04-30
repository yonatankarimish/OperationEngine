package com.sixsense.model.events;

import com.sixsense.model.retention.ResultRetention;
import com.sixsense.io.Session;

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
