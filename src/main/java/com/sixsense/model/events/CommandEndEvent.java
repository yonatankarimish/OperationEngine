package com.sixsense.model.events;

import com.sixsense.model.commands.Command;
import com.sixsense.model.logic.ExpressionResult;
import com.sixsense.io.Session;

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
