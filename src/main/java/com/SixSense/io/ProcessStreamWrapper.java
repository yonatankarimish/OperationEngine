package com.SixSense.io;

import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;

public class ProcessStreamWrapper implements Callable<Boolean> {
    private static Logger logger = Logger.getLogger(ProcessStreamWrapper.class);

    private Session session;
    private InputStream processStream;
    private final List<String> commandResult;


    public ProcessStreamWrapper(InputStream processStream, Session session, List<String> commandResult) {
        this.processStream = processStream;
        this.session = session;
        this.commandResult = commandResult;
    }

    @Override
    public Boolean call() throws IOException{
        String lastResponse;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(processStream))){
            do {
                lastResponse = reader.readLine();
                logger.info("read line " + lastResponse + " directly from stream");
                if(lastResponse != null) {
                    synchronized (this.commandResult){
                        this.commandResult.add(lastResponse);
                    }

                    if(lastResponse.equals(this.session.getCommandEndIdentifier())){
                        this.session.signalCommandFinished();
                    }
                }
            } while (lastResponse != null);

            logger.info("finished reading from stream");
            return true;
        } catch (IOException e){
            logger.error("Failed to process command " + this.session.getTerminalIdentifier() + ". Caused by: ", e);
            throw e;
        }
    }
}