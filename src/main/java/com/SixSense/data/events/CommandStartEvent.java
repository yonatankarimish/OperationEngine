package com.SixSense.data.events;

import com.SixSense.data.commands.Command;
import com.SixSense.io.Session;

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
