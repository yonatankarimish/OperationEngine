package com.sixsense.model.pipes;

import com.sixsense.model.logging.Loggers;
import com.sixsense.model.retention.RetentionType;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.io.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.Iterator;
import java.util.List;

//Drains the contents of the output list to the file provided in the ResultRetention of the current command
public class DrainingPipe extends AbstractOutputPipe {
    private static final Logger logger = LogManager.getLogger(Loggers.FileLogger.name());
    private final String DEFAULT_DRAIN_FILE = "current_file.txt";

    public DrainingPipe(){}

    @Override
    public List<String> pipe(Session session, List<String> output) {
        if(output == null || output.isEmpty()){
            return output;
        }

        String fileName;
        ResultRetention commandRetention = session.getCurrentCommand().getSaveTo();
        if(commandRetention.getRetentionType().equals(RetentionType.File)){
            fileName = commandRetention.getName();
        }else{
            fileName = DEFAULT_DRAIN_FILE;
        }

        ThreadContext.put("logFile", fileName);
        Iterator<String> it = output.iterator();
        while (it.hasNext()){
            String nextLine = it.next();
            if (!nextLine.contains(session.getCurrentEvaluatedCommand()) && !nextLine.contains(session.getCurrentPrompt()) && it.hasNext()) {
                logger.info(nextLine);
                it.remove();
            }
        }
        ThreadContext.remove("logFile");
        return output;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DrainingPipe;
    }
}
