package com.sixsense.services;

import com.sixsense.model.events.*;
import com.sixsense.model.logging.Loggers;
import com.sixsense.model.logic.ExpressionResult;
import com.sixsense.model.retention.DatabaseVariable;
import com.sixsense.model.retention.RetentionMode;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.io.Session;
import com.sixsense.utillity.CommandUtils;
import com.sixsense.utillity.ExpressionUtils;
import com.sixsense.utillity.Literals;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.routing.RoutingAppender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Service
/*Manages all logging operations that are done from a session's context*/
public class LoggingManager  {
    private static final EnumMap<Loggers, Logger> loggers = new EnumMap<>(Loggers.class); //Maps logger names to the actual loggers
    private static final Map<String, RoutingAppender> routingAppenders = new HashMap<>(); //Routing appenders (holding references to files belonging to sessions)

    private LoggingManager(){
        for(Loggers logger : EnumSet.allOf(Loggers.class)){
            loggers.put(logger, LogManager.getLogger(logger.name()));
        }

        Map<String, Appender> appenderMap = ((LoggerContext) LogManager.getContext(false)).getConfiguration().getAppenders();
        routingAppenders.put("session_log", (RoutingAppender)appenderMap.get("session_log"));
        routingAppenders.put("command_log", (RoutingAppender)appenderMap.get("command_log"));
        routingAppenders.put("terminal_log", (RoutingAppender)appenderMap.get("terminal_log"));
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

    void closeLoggers(String sessionId){
        routingAppenders.values().forEach(appender -> {
            FileAppender fileAppender = (FileAppender) appender.getAppenders().get(sessionId).getAppender();
            FileManager fileManager = fileAppender.getManager();
            fileManager.close();
        });
    }

    private void logSessionCreated(SessionCreatedEvent event){
        String indentation = getIndentation(event.getSession());
        String creationDate = Instant.now().atZone(ZoneId.of("CET")).toLocalDateTime().format(Literals.DateFormatter);

        jointLog(
            EnumSet.of(Loggers.SessionLogger, Loggers.CommandLogger),
            Level.INFO,
            "Session " +  event.getSession().getSessionShellId() + " has been created at " + creationDate
        );
        logDynamicFields(indentation, SessionEngine.getSessionProperties(), Literals.PlusSign);
    }

    private void logOperationStart(OperationStartEvent event){
        String indentation = getIndentation(event.getSession());
        loggers.get(Loggers.SessionLogger).info(indentation + "Operation " + event.getOperation().getShortUUID() + " Start");
        logDynamicFields(indentation, event.getOperation().getDynamicFields(), Literals.PlusSign);
    }

    private void logBlockStart(BlockStartEvent event){
        String indentation = getIndentation(event.getSession());
        loggers.get(Loggers.SessionLogger).info(indentation + "Block " + event.getBlock().getShortUUID() + " Start");
        logDynamicFields(indentation, event.getBlock().getDynamicFields(), Literals.PlusSign);
    }

    private void logCommandStart(CommandStartEvent event){
        String indentation = getIndentation(event.getSession());
        loggers.get(Loggers.SessionLogger).info(indentation + "Command " + event.getCommand().getShortUUID() + " Start");
        logDynamicFields(indentation, event.getCommand().getDynamicFields(), Literals.PlusSign);
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
        logDynamicFields(indentation, event.getCommand().getDynamicFields(), Literals.MinusSign);
        loggers.get(Loggers.SessionLogger).info(indentation + "Command result is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info(indentation + "Command " + event.getCommand().getShortUUID() + " End");
    }

    private void logBlockEnd(BlockEndEvent event){
        String indentation = getIndentation(event.getSession());
        logDynamicFields(indentation, event.getBlock().getDynamicFields(), Literals.MinusSign);
        loggers.get(Loggers.SessionLogger).info(indentation + "Block result is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info(indentation + "Block " + event.getBlock().getShortUUID() + " End");
    }

    private void logOperationEnd(OperationEndEvent event){
        String indentation = getIndentation(event.getSession());
        logDynamicFields(indentation, event.getOperation().getDynamicFields(), Literals.MinusSign);
        loggers.get(Loggers.SessionLogger).info(indentation + "Operation result is " + event.getResult());
        loggers.get(Loggers.SessionLogger).info(indentation + "Operation " + event.getOperation().getShortUUID() + " End");
    }

    private void logSessionClosed(SessionClosedEvent event){
        String indentation = getIndentation(event.getSession());
        logDynamicFields(indentation, SessionEngine.getSessionProperties(), Literals.MinusSign);
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
        RetentionMode retentionMode = retention.getRetentionMode();

        loggers.get(Loggers.SessionLogger).info(indentation + "Result retention of type [" + retentionMode.name() + "]");
        switch (retentionMode){
            case Variable:{
                Map<String, String> oldSessionVarState = session.getCurrentSessionVariables();
                if(oldSessionVarState.containsKey(retention.getName())) {
                    logDynamicFields(indentation, retention.getName(), oldSessionVarState.get(retention.getName()), Literals.MinusSign);
                }

                logDynamicFields(indentation, retention.getName(), retention.getValue(), Literals.PlusSign);
            }break;
            case File:{
                loggers.get(Loggers.SessionLogger).info(indentation + "Added results to file " + retention.getName());
            }break;
            case DatabaseEventual:{
                Set<DatabaseVariable> oldDatabaseVarState = session.getDatabaseVariables();
                for(DatabaseVariable var : oldDatabaseVarState) {
                    if (var.getName().equals(retention.getName())) {
                        logDynamicFields(indentation, retention.getName(), var.getValue(), Literals.MinusSign);
                        break;
                    }
                }

                logDynamicFields(indentation, retention.getName(), retention.getValue(), Literals.PlusSign);
            }break;
            case DatabaseImmediate:{
                logDynamicFields(indentation, retention.getName(), retention.getValue(), Literals.PlusSign);
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
        return Literals.Tab.repeat(session.getDrilldownRank());
    }

    //The dynamic fields should be from the ICommand in question, and not all fields in session context. because we log loading and removal of the relevant fields by the ICommand
    private void logDynamicFields(String indentation, Map<String, String> dynamicFields, String sign){
        if(dynamicFields.size() > 0) {
            for (Map.Entry<String, String> dynamicField : dynamicFields.entrySet()) {
                logDynamicFields(indentation, dynamicField.getKey(), dynamicField.getValue(), sign);
            }
        }
    }

    private void logDynamicFields(String indentation, String key, String value, String sign){
        loggers.get(Loggers.SessionLogger).info(indentation + sign + " " + key + " = \"" + value + "\"");
    }

    private void jointLog(EnumSet<Loggers> loggerNames, Level logLevel, String message){
        for(Loggers loggerName : loggerNames){
            loggers.get(loggerName).log(logLevel, message);
        }
    }
}
