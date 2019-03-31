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

    /*ProcessStreamWrapper runs in a separate thread than the session that created it
    * If the process output/error stream fills it's own buffer, the process will get stuck and no new commands may be written to it
    * ProcessStreamWrapper will continuously read from the process stream it receives in the constructor, to avoid the process jamming and failing the session*/
    public ProcessStreamWrapper(InputStream processStream, Session session, List<String> commandResult) {
        this.processStream = processStream;
        this.session = session;
        this.commandResult = commandResult;
    }

    @Override
    public Boolean call() throws IOException{
        String lastResponse;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(processStream))){
            logger.debug("started reading from stream for session " + this.session.getTerminalIdentifier());
            do {
                lastResponse = reader.readLine();
                if(lastResponse != null) {
                    logger.debug("read line " + lastResponse + " directly from stream");
                    synchronized (this.commandResult) {
                        this.commandResult.add(lastResponse);
                    }
                    if (lastResponse.equals(this.session.getCommandEndIdentifier())) {
                        try{
                            this.session.getCommandLock().lock();
                            //logger.debug(this.session.getTerminalIdentifier() + " proccess stream acquired lock");
                            this.session.getCommandOutputFinished().signalAll();
                        }finally {
                            this.session.getCommandLock().unlock();
                            //logger.debug(this.session.getTerminalIdentifier() + " proccess stream released lock");
                        }
                    }
                }
            } while (lastResponse != null);

            logger.debug("finished reading from stream for session " + this.session.getTerminalIdentifier());
            return true;
        } catch (Exception e){
            logger.error("Failed to process command " + this.session.getTerminalIdentifier() + ". Caused by: ", e);
            throw new IOException(e);
        }
    }
}