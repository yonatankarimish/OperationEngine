package com.SixSense.data.events;

import com.SixSense.data.commands.Command;
import com.SixSense.io.Session;

public class InputSentEvent extends AbstractEngineEvent {
    private Command command;
    private String inputSent;

    public InputSentEvent(Session session, Command command, String inputSent) {
        super(EngineEventType.InputSent, session);
        this.command = command.deepClone();
        this.inputSent = inputSent;
    }

    public Command getCommand() {
        return command;
    }

    public String getInputSent() {
        return inputSent;
    }
}
