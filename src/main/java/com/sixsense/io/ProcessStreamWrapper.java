package com.sixsense.io;

import com.sixsense.model.logging.IDebuggable;
import com.sixsense.model.logging.Loggers;
import com.sixsense.utillity.Literals;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;

public class ProcessStreamWrapper implements Closeable, Supplier<Boolean>, IDebuggable {
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
    private boolean isClosed = false;

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
        this.substitutionCriteria.put(" *" + Literals.CarriageReturn + " *", ""); //trim all space characters around carriage returns
        this.substitutionCriteria.put(" *" + Literals.LineBreak + " *", Literals.LineBreak); //while retaining the line breaks
    }

    @Override
    public Boolean get(){
        ThreadContext.put("sessionID", this.session.getShortSessionId());
        logger.debug("started reading from stream for session " + this.session.getSessionShellId());
        byte[] rawDataBuffer = new byte[1024];
        int bytesRead;

        do {
            bytesRead = readIntoBuffer(rawDataBuffer);

            if(bytesRead != -1) {
                String currentChunk = parseRawChunk(rawDataBuffer, bytesRead);
                String chunkAfterSubstitution = executeSubstitutionCriteria(currentChunk);
                String[] splitChunk = chunkAfterSubstitution.split(Literals.LineBreak, -1);  //splitChunk will always have at least one entry (if no line break was read); passing -1 as the second argument will perform the maximum amount of possible splits, without omitting leading or trailing empty strings
                addChunksToOutput(splitChunk);
                signalNewChunk();
            }
        } while (bytesRead != -1 && !this.isClosed()); //as long as eof wasn't reached and the process stream wasn't closed (these conditions are independent)

        logger.debug("finished reading from stream for session " + this.session.getSessionShellId());
        ThreadContext.remove("sessionID");
        return true;
    }

    private int readIntoBuffer(byte[] rawDataBuffer){
        try {
            return this.processStream.read(rawDataBuffer);
        } catch (IOException e) {
            /*processStream.read() will throw an IO exception if closed while waiting for bytes.
            * if the synchronization is held by another thread (invoking close()) we wait for it to finish before checking for closure reason */
            synchronized (this) {
                //verify the exception didn't happen due to manual termination or due to a natural session.close()
                if (!this.isClosed() && !this.session.isTerminated()) {
                    logger.error("Failed to process command " + this.session.getTerminalIdentifier() + ". Caused by: " + e.getMessage());
                }
            }
            return -1; //will skip the condition and exit the loop in the get() method
        }
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
            for(Map.Entry<String, String> criterion : this.substitutionCriteria.entrySet()) {
                String pattern = criterion.getKey();
                String replacement = criterion.getValue();
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
        if(!this.isClosed()) {
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

    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void activateDebugMode() {
        isUnderDebug = true;
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            processStream.close();
            this.isClosed = true;
        }catch (IOException e){
            logger.error("Failed to close process stream for session " + this.session.getShortSessionId() + ". Caused by: " + e.getMessage());
            throw e;
        }
    }
}