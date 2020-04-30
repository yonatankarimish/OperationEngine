package com.sixsense.utillity;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.time.format.DateTimeFormatter;

public class MessageLiterals {
    private static final Logger logger = LogManager.getLogger(MessageLiterals.class);
    public static final String projectDirectory;
    static{
        String tmpDirectory = "/";
        try {
            tmpDirectory =  new File(MessageLiterals.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (URISyntaxException e) {
            logger.warn("Failed to resolve current project directory. Resolving as root directory. Caused by: " + e.getMessage());
        }finally {
            projectDirectory = tmpDirectory;
        }
    }

    public static final String FileSeparator = FileSystems.getDefault().getSeparator();
    public static final String CarriageReturn = "\r";
    public static final String LineBreak = System.lineSeparator();
    public static final String Tab = "\t";
    public static final String VariableMark = "$";

    public static final String DateTimeFormat = "dd-MM-yyyy HH:mm:ss";
    public static final DateTimeFormatter DateFormatter = DateTimeFormatter.ofPattern(DateTimeFormat);

    public static final String configFilesPath = projectDirectory + "/config";
    public static final String SessionExecutionDir = projectDirectory + "/logs/sessions/";

    public static final String EngineShutdown = "Session services has been shut down";
    public static final String ExceptionEncountered = "Session services encountered an error";
    public static final String ExpectedOutcomeNotNumeric = "Expected outcome is not a number";
    public static final String CommandDidNotMatchConditions = "Command did not match it's execution conditions";
    public static final String CommandDidNotReachOutcome = "Command did not reach it's expected outcome";
    public static final String InvalidCommandParameters = "Command has invalid parameters";
    public static final String InvalidExecutionBlock = "Execution block is not a valid block";
    public static final String OperationTerminated = "Operation has been terminated externally";
    public static final String UnsuportedBinaryRelation = "Expected outcome has an unsupported binary relation";
    public static final String ThreadNotMonitored = "Attempted to update thread state for a non-monitored thread";
    public static final String TimeoutInCommand = "Command did not return within it's specified time limit";
}
