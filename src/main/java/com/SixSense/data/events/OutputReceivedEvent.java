package com.SixSense.data.events;

import com.SixSense.data.commands.Command;
import com.SixSense.io.Session;

public class OutputReceivedEvent extends AbstractEngineEvent {
    private Command command;
    private String outputReceived;

    public OutputReceivedEvent(Session session, Command command, String outputReceived) {
        super(EngineEventType.OutputReceived, session);
        this.command = command.deepClone();
        this.outputReceived = outputReceived;
    }

    public Command getCommand() {
        return command;
    }

    public String getOutputReceived() {
        return outputReceived;
    }
}
