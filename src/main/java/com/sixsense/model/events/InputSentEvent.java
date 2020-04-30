package com.sixsense.model.events;

import com.sixsense.model.commands.Command;
import com.sixsense.io.Session;

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
