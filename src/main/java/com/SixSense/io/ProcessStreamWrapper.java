package com.SixSense.io;

import com.SixSense.util.MessageLiterals;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;

public class ProcessStreamWrapper implements Callable<Boolean> {
    private static Logger logger = Logger.getLogger(ProcessStreamWrapper.class);

    private Session session;
    private InputStream processStream;
    private final List<String> processOutput;

    /*ProcessStreamWrapper runs in a separate thread than the session that created it
    * If the process output/error stream fills it's own buffer, the process will get stuck and no new commands may be written to it
    * ProcessStreamWrapper will continuously read from the process stream it receives in the constructor, to avoid the process jamming and failing the session
    * Each session has two wrappers - one for the local process and one for the remote process*/
    public ProcessStreamWrapper(InputStream processStream, Session session, List<String> processOutput) {
        this.processStream = processStream;
        this.session = session;
        this.processOutput = processOutput;
    }

    @Override
    public Boolean call() throws IOException{
        try {
            logger.debug("started reading from stream for session " + this.session.getSessionShellId());
            byte[] rawData = new byte[1024];
            int bytesRead;
            do {
                bytesRead = this.processStream.read(rawData);
                if(bytesRead != -1) {
                    String currentChunk = new String(rawData, 0, bytesRead);
                    logger.debug("read chunk " + currentChunk + " directly from stream");
                    synchronized (this.processOutput) {
                        //splitChunk will always have at least one entry (if no line break was read)
                        String[] splitChunk = (" "  + currentChunk  //pad the current chunk with whitespace to split on leading line breaks
                                .replace(" *"+MessageLiterals.CarriageReturn+" *", "") //trim all space characters around carriage returns
                                .replace(" *"+MessageLiterals.CarriageReturn+MessageLiterals.LineBreak+" *", MessageLiterals.LineBreak) //while retaining the line breaks
                                + " ")// then pad the current chunk with whitespace to split on trailing line breaks
                                .split(MessageLiterals.LineBreak); //so we can split on them here

                        if(splitChunk.length > 0) {
                            String firstChunk = splitChunk[0].trim();
                            if (this.processOutput.isEmpty()) {
                                this.processOutput.add(firstChunk);
                            } else {
                                String lastLine = this.processOutput.get(this.processOutput.size() - 1);
                                this.processOutput.set(this.processOutput.size() - 1, lastLine + firstChunk);
                            }

                            int chunkIdx;
                            for (chunkIdx = 1; chunkIdx < splitChunk.length; chunkIdx++) {
                                this.processOutput.add(splitChunk[chunkIdx].trim());
                            }
                        }
                    }

                    if(!this.session.isClosed()) {
                        this.session.getCommandLock().lock();
                        logger.debug(this.session.getTerminalIdentifier() + " proccess stream acquired lock");
                        try {
                            this.session.getNewChunkReceived().signalAll();
                        } finally {
                            this.session.getCommandLock().unlock();
                            logger.debug(this.session.getTerminalIdentifier() + " proccess stream released lock");
                        }
                    }
                }
            } while (bytesRead != -1);

            logger.debug("finished reading from stream for session " + this.session.getSessionShellId());
        } catch (Exception e){
            if(!this.session.isClosed()) {
                logger.error("Failed to process command " + this.session.getTerminalIdentifier() + ". Caused by: ", e);
                throw new IOException(e);
            }
        }
        return true;
    }
}