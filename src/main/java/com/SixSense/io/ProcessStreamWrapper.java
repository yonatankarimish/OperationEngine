package com.SixSense.io;

import com.SixSense.data.logging.IDebuggable;
import com.SixSense.data.logging.Loggers;
import com.SixSense.util.MessageLiterals;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;

public class ProcessStreamWrapper implements Supplier<Boolean>, IDebuggable {
    //Loggers
    private static final Logger logger = LogManager.getLogger(ProcessStreamWrapper.class);
    private static final Logger terminalLogger = LogManager.getLogger(Loggers.TerminalLogger.name());

    //Session and I/O
    private Session session; //parent session
    private InputStream processStream; //JVM input stream (i.e. terminal output stream)
    private final List<String> processOutput; //List representation of the parsed output

    //Diagnostics
    private final List<String> rawChunks;
    private final Map<String, String> substitutionCriteria;
    private boolean isUnderDebug = false;

    /*ProcessStreamWrapper runs in a separate thread than the session that created it
    * If the process output/error stream fills it's own buffer, the process will get stuck and no new commands may be written to it
    * ProcessStreamWrapper will continuously read from the process stream it receives in the constructor, to avoid the process jamming and failing the session
    * Each session has a map of open channels, through which it performs I/O with the required channel*/
    ProcessStreamWrapper(InputStream processStream, Session session, List<String> processOutput) {
        this.processStream = processStream;
        this.session = session;
        this.processOutput = processOutput;
        this.rawChunks = new ArrayList<>();
        this.substitutionCriteria = new LinkedHashMap<>();

        //Initialize the default substitution criteria
        this.substitutionCriteria.put(" *" + MessageLiterals.CarriageReturn + " *", ""); //trim all space characters around carriage returns
        this.substitutionCriteria.put(" *" + MessageLiterals.LineBreak + " *", MessageLiterals.LineBreak); //while retaining the line breaks
    }

    @Override
    public Boolean get(){
        try {
            ThreadContext.put("sessionID", this.session.getSessionShellId());
            logger.debug("started reading from stream for session " + this.session.getSessionShellId());
            byte[] rawData = new byte[1024];
            int bytesRead;
            do {
                bytesRead = this.processStream.read(rawData);
                if(bytesRead != -1) {
                    String currentChunk = parseRawChunk(rawData, bytesRead);
                    String chunkAfterSubstitution = executeSubstitutionCriteria(currentChunk);
                    String[] splitChunk = chunkAfterSubstitution.split(MessageLiterals.LineBreak, -1);  //splitChunk will always have at least one entry (if no line break was read); passing -1 as the second argument will perform the maximum amount of possible splits, without omitting leading or trailing empty strings
                    addChunksToOutput(splitChunk);
                    signalNewChunk();
                }
            } while (bytesRead != -1);

            logger.debug("finished reading from stream for session " + this.session.getSessionShellId());
        } catch (Exception e) {
            if(!this.session.isClosed() && !this.session.isTerminated()) {
                logger.error("Failed to process command " + this.session.getTerminalIdentifier() + ". Caused by: ", e);
            }
        } finally {
            ThreadContext.remove("sessionID");
        }
        return true;
    }

    /*If any bytes have been read into the byte buffer, construct a string using the bytes in the buffer
     * add the chunk into the raw chunks list
     * then log that it has been parsed*/
    private String parseRawChunk(byte[] rawData, int bytesRead){
        String currentChunk = new String(rawData, 0, bytesRead);
        if(this.isUnderDebug) {
            synchronized (this.rawChunks) {
                rawChunks.add(currentChunk);
            }
            logger.debug("read chunk " + currentChunk + " directly from stream");
        }
        terminalLogger.info(currentChunk);
        return currentChunk;
    }

    /*Execute the substitution criteria against the current chunk*/
    private String executeSubstitutionCriteria(String currentChunk){
        synchronized (this.substitutionCriteria) {
            for(String pattern : this.substitutionCriteria.keySet()) {
                String replacement = this.substitutionCriteria.get(pattern);
                currentChunk = currentChunk.replaceAll(pattern, replacement);
            }
        }

        return currentChunk;
    }

    /*Add the split chunks into the list representation of the output*/
    private void addChunksToOutput(String[] splitChunk){
        synchronized (this.processOutput) {
            if(splitChunk.length > 0) {
                String firstChunk = splitChunk[0];
                if (this.processOutput.isEmpty()) {
                    this.processOutput.add(firstChunk);
                } else {
                    String lastLine = this.processOutput.get(this.processOutput.size() - 1);
                    this.processOutput.set(this.processOutput.size() - 1, lastLine + firstChunk);
                }

                int chunkIdx;
                for (chunkIdx = 1; chunkIdx < splitChunk.length; chunkIdx++) {
                    this.processOutput.add(splitChunk[chunkIdx]);
                }
            }
        }
    }

    /*Signal the parent session that new chunks has been parsed (i.e. there is new output)*/
    private void signalNewChunk(){
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

    public List<String> getRawChunks(){
        synchronized (this.rawChunks) {
            return Collections.unmodifiableList(this.rawChunks);
        }
    }

    public Map<String, String> getSubstitutionCriteria(){
        synchronized (this.substitutionCriteria) {
            return Collections.unmodifiableMap(this.substitutionCriteria);
        }
    }

    public ProcessStreamWrapper addSubstitutionCriteria(String regex, String replacement){
        synchronized (this.substitutionCriteria) {
            this.substitutionCriteria.put(regex, replacement);
            return this;
        }
    }

    @Override
    public boolean isUnderDebug() {
        return isUnderDebug;
    }

    @Override
    public void activateDebugMode() {
        isUnderDebug = true;
    }
}