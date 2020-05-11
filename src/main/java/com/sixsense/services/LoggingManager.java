package com.sixsense.services;

import com.sixsense.model.events.*;
import com.sixsense.model.logging.Loggers;
import com.sixsense.model.logic.ExpressionResult;
import com.sixsense.model.retention.RetentionType;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.io.Session;
import com.sixsense.utillity.CommandUtils;
import com.sixsense.utillity.ExpressionUtils;
import com.sixsense.utillity.MessageLiterals;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
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
                case ConditionEvaluation: logConditionEvaluation((ConditionEvaluationEvent) event); break;
                case OutcomeEvaluation: logOutcomeEvaluation((OutcomeEvaluationEvent) event); break;
                case ResultRetention: logVariableRetention((ResultRetentionEvent)event); break;
                case ExecutionAnomaly: logExecutionAnomaly((ExecutionAnomalyEvent)event); break;
                default: throw new IllegalArgumentException("Cannot log event of type " + event.getEventType() + ": Event type does not exist");
            }
        }catch(ClassCastException e){
            throw new IllegalArgumentException("Cannot log event of type " + event.getEventType() + ": Event object has illegal configuration");
        }
    }

    private void logSessionCreated(SessionCreatedEvent event){
        String creationDate = Instant.now().atZone(ZoneId.of("CET")).toLocalDateTime().format(MessageLiterals.DateFormatter);

        jointLog(
            EnumSet.of(Loggers.SessionLogger, Loggers.CommandLogger),
            Level.INFO,
            "Session " +  event.getSession().getSessionShellId() + " has been created at " + creationDate
        );
        logDynamicFields(event.getSession(), SessionEngine.getSessionProperties(), '+');
    }

    private void logOperationStart(OperationStartEvent event){
        String indentation = getIndentation(event.getSession());
        loggers.get(Loggers.SessionLogger).info(indentation + "Operation " + event.getOperation().getShortUUID() + " Start");
        logDynamicFields(event.getSession(), event.getOperation().getDynamicFields(), '+');
    }

    private void logBlockStart(BlockStartEvent event){
        String indentation = getIndentation(event.getSession());
        loggers.get(Loggers.SessionLogger).info(indentation + "Block " + event.getBlock().getShortUUID() + " Start");
        logDynamicFields(event.getSession(), event.getBlock().getDynamicFields(), '+');
    }

    private void logCommandStart(CommandStartEvent event){
        String indentation = getIndentation(event.getSession());
        loggers.get(Loggers.SessionLogger).info(indentation + "Command " + event.getCommand().getShortUUID() + " Start");
        logDynamicFields(event.getSession(), event.getCommand().getDynamicFields(), '+');
    }

    private void logInputSent(InputSentEvent event){
        String indentation = getIndentation(event.getSession());
        loggers.get(Loggers.SessionLogger).debug(indentation + event.getSession().getTerminalIdentifier() + " session acquired lock");
        loggers.get(Loggers.SessionLogger).info(indentation + "Wrote: \"" + event.getInputSent() + "\" to channel " + event.getCommand().getChannelName());
        loggers.get(Loggers.CommandLogger).info(event.getOrdinal() + "W): <" + event.getCommand().getChannelName() + "> " + event.getInputSent());
    }

    private void logOutputReceived(OutputReceivedEvent event){
        String indentation = getIndentation(event.getSession());
        loggers.get(Loggers.SessionLogger).debug(indentation + event.getSession().getTerminalIdentifier() + " session finished command wait");
        loggers.get(Loggers.SessionLogger).info(indentation + "Read: \"" + event.getOutputReceived() + "\"");
        loggers.get(Loggers.CommandLogger).info(event.getOrdinal() + "R): <" + event.getCommand().getChannelName() + "> " + event.getOutputReceived());
        loggers.get(Loggers.SessionLogger).debug(indentation + event.getSession().getTerminalIdentifier() + " session released lock");
    }

    private void logCommandEnd(CommandEndEvent event){
        String indentation = getIndentation(event.getSession());
        logDynamicFields(event.getSession(), event.getCommand().getDynamicFields(), '-');
        loggers.get(Loggers.SessionLogger).info(indentation + "Command result is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info(indentation + "Command " + event.getCommand().getShortUUID() + " End");
    }

    private void logBlockEnd(BlockEndEvent event){
        String indentation = getIndentation(event.getSession());
        logDynamicFields(event.getSession(), event.getBlock().getDynamicFields(), '-');
        loggers.get(Loggers.SessionLogger).info(indentation + "Block result is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info(indentation + "Block " + event.getBlock().getShortUUID() + " End");
    }

    private void logOperationEnd(OperationEndEvent event){
        String indentation = getIndentation(event.getSession());
        logDynamicFields(event.getSession(), event.getOperation().getDynamicFields(), '-');
        loggers.get(Loggers.SessionLogger).info(indentation + "Operation result is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info(indentation + "Operation " + event.getOperation().getShortUUID() + " End");
    }

    private void logSessionClosed(SessionClosedEvent event){
        logDynamicFields(event.getSession(), SessionEngine.getSessionProperties(), '-');
        jointLog(
            EnumSet.of(Loggers.SessionLogger, Loggers.CommandLogger),
            Level.INFO,
            "Session " +  event.getSession().getSessionShellId() + " has been closed"
        );
    }

    private void logConditionEvaluation(ConditionEvaluationEvent event){
        String indentation = getIndentation(event.getSession());
        String asTree = ExpressionUtils.toPrintableString(event.getCondition()).replaceAll("\n", "\n" + indentation);
        String resolvedCondition = CommandUtils.evaluateAgainstDynamicFields(asTree, event.getSession().getCurrentSessionVariables());
        loggers.get(Loggers.SessionLogger).info(indentation + "Execution condition:");
        loggers.get(Loggers.SessionLogger).info(indentation + resolvedCondition);
    }

    private void logOutcomeEvaluation(OutcomeEvaluationEvent event){
        String indentation = getIndentation(event.getSession());
        String asTree = ExpressionUtils.toPrintableString(event.getExpectedOutcome()).replaceAll("\n", "\n" + indentation);
        String resolvedOutcome = CommandUtils.evaluateAgainstDynamicFields(asTree, event.getSession().getCurrentSessionVariables());
        loggers.get(Loggers.SessionLogger).info(indentation + "Expected outcome:");
        loggers.get(Loggers.SessionLogger).info(indentation + resolvedOutcome);
    }

    private void logVariableRetention(ResultRetentionEvent event){
        Session session = event.getSession();
        String indentation = getIndentation(session);
        ResultRetention retention = event.getResultRetention();
        RetentionType retentionType = retention.getRetentionType();
        loggers.get(Loggers.SessionLogger).info(indentation + "Result retention of type [" + retentionType.name() + "]");
        switch (retentionType){
            case Variable:{
                Map<String, String> oldSessionVarState = session.getCurrentSessionVariables();
                if(oldSessionVarState.containsKey(retention.getName())) {
                    Map<String, String> oldValue = Collections.singletonMap(retention.getName(), oldSessionVarState.get(retention.getName()));
                    logDynamicFields(session, oldValue, '-');
                }

                Map<String, String> newValue = Collections.singletonMap(retention.getName(), retention.getValue());
                logDynamicFields(session, newValue, '+');
            }break;
            case File:{
                loggers.get(Loggers.SessionLogger).info(indentation + "Added results to file " + retention.getName());
            }break;
            case DatabaseEventual:{
                Map<String, String> oldDatabaseVarState = session.getDatabaseVariables();
                if(oldDatabaseVarState.containsKey(retention.getName())) {
                    Map<String, String> oldValue = Collections.singletonMap(retention.getName(), oldDatabaseVarState.get(retention.getName()));
                    logDynamicFields(session, oldValue, '-');
                }

                Map<String, String> newValue = Collections.singletonMap(retention.getName(), retention.getValue());
                logDynamicFields(session, newValue, '+');
            }break;
            case DatabaseImmediate:{
                Map<String, String> newValue = Collections.singletonMap(retention.getName(), retention.getValue());
                logDynamicFields(session, newValue, '+');
            }break;
            default:{
                loggers.get(Loggers.SessionLogger).info(indentation + "No result retention was performed");
            }break;
        }
    }

    private void logExecutionAnomaly(ExecutionAnomalyEvent event){
        ExpressionResult anomaly = event.getResult();
        loggers.get(Loggers.SessionLogger).error("Excecution anomaly encountered: " + anomaly.toString());
    }

    private String getIndentation(Session session){
        return MessageLiterals.Tab.repeat(session.getDrilldownRank());
    }

    //The dynamic fields should be from the ICommand in question, and not all fields in session context. because we log loading and removal of the relevant fields by the ICommand
    private void logDynamicFields(Session session, Map<String, String> dynamicFields, Character sign){
        if(dynamicFields.size() > 0) {
            String indentation = getIndentation(session);
            for (String dynamicField : dynamicFields.keySet()) {
                loggers.get(Loggers.SessionLogger).info(indentation + sign + " " + dynamicField + " = \"" + dynamicFields.get(dynamicField) + "\"");
            }
        }
    }

    private void jointLog(EnumSet<Loggers> loggerNames, Level logLevel, String message){
        for(Loggers loggerName : loggerNames){
            loggers.get(loggerName).log(logLevel, message);
        }
    }
}