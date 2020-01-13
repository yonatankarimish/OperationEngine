package com.SixSense.data.events;

import com.SixSense.data.commands.Command;
import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.io.Session;

public class CommandEndEvent extends AbstractEngineEvent {
    private Command command;
    private ExpressionResult result;

    public CommandEndEvent(Session session, Command command, ExpressionResult result) {
        super(EngineEventType.CommandEnd, session);
        this.command = command;
        this.result = result;
    }

    public Command getCommand() {
        return command;
    }

    public ExpressionResult getResult() {
        return result;
    }
}
