package com.SixSense.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;

public class MessageLiterals {
    private static Logger logger = Logger.getLogger(MessageLiterals.class);
    public static final String FileSeparator = FileSystems.getDefault().getSeparator();
    public static final String LineBreak = System.lineSeparator();


    public static final String SessionPropertiesPath = projectDirectory() + "/config/sixsense.session.properties";

    public static final String ExpectedOutcomeNotNumeric = "Expected outcome is not a number";
    public static final String CommandDidNotReachOutcome = "Command did not reach any of it's expected outcomes";
    public static final String InvalidCommandParameters = "Command has invalid parameters";
    public static final String InvalidExecutionBlock = "Execution block is not a valid block";
    public static final String UnsuportedBinaryRelation = "Expected outcome has an unsupported binary relation";
    public static final String TimeoutInCommand = "Command did not return within it's specified time limit";

    private static String projectDirectory(){
        String projectDir;
        try {
            return new File(MessageLiterals.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (URISyntaxException e) {
            logger.error("Failed to resolve current project directory. Resolving as root directory. Caused by: ", e);
            return "/";
        }
    }
}
