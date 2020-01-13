package com.SixSense.data.events;

import com.SixSense.data.commands.Command;
import com.SixSense.io.Session;

public class InputSentEvent extends AbstractEngineEvent {
    private Command command;
    private int ordinal;
    private String inputSent;

    public InputSentEvent(Session session, Command command, int ordinal, String inputSent) {
        super(EngineEventType.InputSent, session);
        this.command = command;
        this.ordinal = ordinal;
        this.inputSent = inputSent;
    }

    public Command getCommand() {
        return command;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public String getInputSent() {
        return inputSent;
    }
}
