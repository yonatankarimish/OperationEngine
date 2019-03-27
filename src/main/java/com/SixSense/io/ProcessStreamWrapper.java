package com.SixSense.io;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ProcessStreamWrapper implements Callable<Boolean> {
    private static Logger logger = Logger.getLogger(ProcessStreamWrapper.class);
    private InputStream processStream;
    private Session session;
    private final List<String> commandResult;
    private int commandOrdinal = 1;


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
                this.session.getCommandLock().lock();

                if(session.getCommandOrdinal() <= this.commandOrdinal){
                    boolean lastBufferClear;
                    synchronized (this.commandResult){
                        lastBufferClear = !this.commandResult.contains(session.getCommandEndIdentifier(this.commandOrdinal));
                    }

                    if(!lastBufferClear){
                        try {
                            this.session.getCommandOutputClear().await();
                        } catch (InterruptedException e) {
                            logger.error("ProcessStreamWrapper for  " + this.session.getSessionShellId() + " interrupted while waiting for command " + this.commandOrdinal + "to return.", e);
                        }
                    }
                }

                lastResponse = reader.readLine();
                if(lastResponse != null) {
                    synchronized (this.commandResult){
                        this.commandResult.add(lastResponse);
                    }

                    if(lastResponse.equals(this.session.getCommandEndIdentifier(this.commandOrdinal))){
                        this.session.getCommandOutputFinished().signalAll();
                        this.commandOrdinal++;
                    }
                }

                this.session.getCommandLock().unlock();
            } while (lastResponse != null);

            return true;
            //return this.sessionResult;
        } catch (IOException e){
            logger.error("Failed to process command " + this.session.generateTerminalIdentifier(this.commandOrdinal) + ". Caused by: ", e);
            throw e;
        }
    }
}