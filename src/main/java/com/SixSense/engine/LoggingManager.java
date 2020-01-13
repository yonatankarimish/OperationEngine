package com.SixSense.engine;

import com.SixSense.data.events.*;
import com.SixSense.data.logging.Loggers;
import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.util.MessageLiterals;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

@Service
/*Manages all logging operations that are done from a session's context*/
public class LoggingManager  {
    private static final EnumMap<Loggers, Logger> loggers = new EnumMap<>(Loggers.class);

    private LoggingManager(){
        for(Loggers logger : EnumSet.allOf(Loggers.class)){
            loggers.put(logger, LogManager.getLogger(logger.name()));
        }
    }

    public void logEngineEvent(AbstractEngineEvent event) {
        try{
            switch (event.getEventType()){
                case SessionCreated: logSessionCreated((SessionCreatedEvent)event); break;
                case OperationStart: logOperationStart((OperationStartEvent)event); break;
                case BlockStart: logBlockStart((BlockStartEvent)event); break;
                case CommandStart: logCommandStart((CommandStartEvent) event); break;
                case InputSent: logInputSent((InputSentEvent) event); break;
                case OutputReceived: logOutputReceived((OutputReceivedEvent) event); break;
                case CommandEnd: logCommandEnd((CommandEndEvent) event); break;
                case BlockEnd: logBlockEnd((BlockEndEvent)event); break;
                case OperationEnd: logOperationEnd((OperationEndEvent)event); break;
                case SessionClosed: logSessionClosed((SessionClosedEvent)event); break;
                case ExecutionAnomaly: logExecutionAnomaly((ExecutionAnomalyEvent)event); break;
                default: throw new IllegalArgumentException("Cannot log event of type " + event.getEventType() + ": Event type does not exist");
            }
        }catch(ClassCastException e){
            throw new IllegalArgumentException("Cannot log event of type " + event.getEventType() + ": Event object has illegal configuration");
        }
    }

    private void logSessionCreated(SessionCreatedEvent event){
        jointLog(
            EnumSet.of(Loggers.SessionLogger, Loggers.CommandLogger),
            Level.INFO,
            "Session " +  event.getSession().getSessionShellId() + " has been created"
        );
        logDynamicFields(SessionEngine.getSessionProperties(), '+');
    }

    private void logOperationStart(OperationStartEvent event){
        loggers.get(Loggers.SessionLogger).info("Operation " + event.getOperation().getShortUUID() + " Start");
        logDynamicFields(event.getOperation().getDynamicFields(), '+');
    }

    private void logBlockStart(BlockStartEvent event){
        loggers.get(Loggers.SessionLogger).info("Block " + event.getBlock().getShortUUID() + " Start");
        logDynamicFields(event.getBlock().getDynamicFields(), '+');
    }

    private void logCommandStart(CommandStartEvent event){
        loggers.get(Loggers.SessionLogger).info("Command " + event.getCommand().getShortUUID() + " Start");
        logDynamicFields(event.getCommand().getDynamicFields(), '+');
    }

    private void logInputSent(InputSentEvent event){
        loggers.get(Loggers.SessionLogger).debug(event.getSession().getTerminalIdentifier() + " session acquired lock");
        loggers.get(Loggers.SessionLogger).info(MessageLiterals.Tab + " Wrote: \"" + event.getInputSent() + "\" to channel " + event.getCommand().getChannelName());
        loggers.get(Loggers.CommandLogger).info(event.getOrdinal() + "W): <" + event.getCommand().getChannelName() + "> " + event.getInputSent());
    }

    private void logOutputReceived(OutputReceivedEvent event){
        loggers.get(Loggers.SessionLogger).debug(event.getSession().getTerminalIdentifier() + " session finished command wait");
        loggers.get(Loggers.SessionLogger).info(MessageLiterals.Tab + " Read: \"" + event.getOutputReceived() + "\"");
        loggers.get(Loggers.CommandLogger).info(event.getOrdinal() + "R): <" + event.getCommand().getChannelName() + "> " + event.getOutputReceived());
        loggers.get(Loggers.SessionLogger).debug(event.getSession().getTerminalIdentifier() + " session released lock");
    }

    private void logCommandEnd(CommandEndEvent event){
        logDynamicFields(event.getCommand().getDynamicFields(), '-');
        loggers.get(Loggers.SessionLogger).info("Command outcome is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info("Command " + event.getCommand().getShortUUID() + " End");
    }

    private void logBlockEnd(BlockEndEvent event){
        logDynamicFields(event.getBlock().getDynamicFields(), '-');
        loggers.get(Loggers.SessionLogger).info("Block outcome is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info("Block " + event.getBlock().getShortUUID() + " End");
    }

    private void logOperationEnd(OperationEndEvent event){
        logDynamicFields(event.getOperation().getDynamicFields(), '-');
        loggers.get(Loggers.SessionLogger).info("Operation outcome is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info("Operation " + event.getOperation().getShortUUID() + " End");
    }

    private void logSessionClosed(SessionClosedEvent event){
        logDynamicFields(SessionEngine.getSessionProperties(), '-');
        jointLog(
            EnumSet.of(Loggers.SessionLogger, Loggers.CommandLogger),
            Level.INFO,
            "Session " +  event.getSession().getSessionShellId() + " has been closed"
        );
    }

    private void logExecutionAnomaly(ExecutionAnomalyEvent event){
        ExpressionResult anomaly = event.getResult();
        loggers.get(Loggers.SessionLogger).error("Excecution anomaly encountered: " + anomaly.toString());
    }

    private void logDynamicFields(Map<String, String> dynamicFields, Character sign){
        if(dynamicFields.size() > 0) {
            for (String dynamicField : dynamicFields.keySet()) {
                loggers.get(Loggers.SessionLogger).info(MessageLiterals.Tab + sign + " " + dynamicField + " = \"" + dynamicFields.get(dynamicField) + "\"");
            }
            loggers.get(Loggers.SessionLogger).info(MessageLiterals.LineBreak);
        }
    }

    private void jointLog(EnumSet<Loggers> loggerNames, Level logLevel, String message){
        for(Loggers loggerName : loggerNames){
            loggers.get(loggerName).log(logLevel, message);
        }
    }
}
