package com.sixsense.model.events;

import com.sixsense.model.commands.Command;
import com.sixsense.io.Session;

public class OutputReceivedEvent extends AbstractEngineEvent {
    private Command command;
    private int ordinal;
    private String outputReceived;

    public OutputReceivedEvent(Session session, Command command, int ordinal, String outputReceived) {
        super(EngineEventType.OutputReceived, session);
        this.command = command;
        this.ordinal = ordinal;
        this.outputReceived = outputReceived;
    }

    public Command getCommand() {
        return command;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public String getOutputReceived() {
        return outputReceived;
    }
}
