package com.SixSense.io;

import com.SixSense.data.logging.Loggers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.function.Supplier;

public class RetentionFileWriter implements Supplier<Boolean> {
    private static final Logger logger = LogManager.getLogger(Loggers.FileLogger.name());
    private final String sessionId;
    private final String fileName;
    private final String contents;

    RetentionFileWriter(String sessionId, String fileName, String contents){
        this.sessionId = sessionId;
        this.fileName = fileName;
        this.contents = contents;
    }

    @Override
    public Boolean get() {
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
