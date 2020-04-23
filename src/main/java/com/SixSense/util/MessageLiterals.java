package com.SixSense.util;

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
            logger.error("Failed to resolve current project directory. Resolving as root directory. Caused by: ", e);
        }finally {
            projectDirectory = tmpDirectory;
        }
    }

    public static final String FileSeparator = FileSystems.getDefault().getSeparator();
    public static final String CarriageReturn = "\r";
    public static final String LineBreak = System.lineSeparator();
    public static final String Tab = "\t";
    public static final String VariableMark = "$";
}
