package com.SixSense.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.Callable;

public class RetentionFileWriter implements Callable<Boolean> {
    private static final Logger logger = LogManager.getLogger(RetentionFileWriter.class);
    private final String sessionId;
    private final String fileName;
    private final String contents;

    RetentionFileWriter(String sessionId, String fileName, String contents){
        this.sessionId = sessionId;
        this.fileName = fileName;
        this.contents = contents;
    }

    @Override
    public Boolean call() {
        try{
            ThreadContext.put("sessionID", this.sessionId);
            ThreadContext.put("logFile", this.fileName);

            logger.info(contents);
            return true;
        }catch (Exception e){
            logger.error(e);
            return false;
        }finally {
            ThreadContext.clearAll();
        }
    }
}
