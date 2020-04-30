package com.sixsense.model.events;

import com.sixsense.model.commands.Command;
import com.sixsense.io.Session;

public class CommandStartEvent extends AbstractEngineEvent {
    private Command command;

    public CommandStartEvent(Session session, Command command) {
        super(EngineEventType.CommandStart, session);
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }
}
