package com.SixSense.util;

import java.nio.file.FileSystems;

public class MessageLiterals {
    public static final String FileSeparator = FileSystems.getDefault().getSeparator();
    public static final String LineBreak = System.lineSeparator();

    public static final String ExpectedOutcomeNotNumeric = "Expected outcome is not a number";
    public static final String InvalidExecutionBlock = "Execution block is not a valid block";
    public static final String UnsuportedBinaryRelation = "Expected outcome has an unsupported binary relation";
    public static final String TimeoutInCommand = "Command did not return within it's specified time limit";
}
