package com.SixSense.io;


import com.SixSense.data.commands.Command;
import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.util.ExpectedOutcomeResolver;
import com.SixSense.util.MessageLiterals;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Session implements Closeable {
    private static Logger logger = Logger.getLogger(Session.class);

    private final Process process; //The operating system process to which we write
    private final List<String> commandOutput; //Line separated response from both the process output stream and the process error stream.
    private Lock commandLock =  new ReentrantLock();
    private final Condition commandOutputFinished = commandLock.newCondition();

    private final UUID sessionShellId = UUID.randomUUID();
    private int commandOrdinal = 0;

    private final BufferedWriter processInput;
    private final ProcessStreamWrapper processOutputAndErrors;


    public Session(Process process){
        this.process = process;
        this.commandOutput = new ArrayList<>();

        this.processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        this.processOutputAndErrors = new ProcessStreamWrapper(process.getInputStream(), this, this.commandOutput);
        logger.info("Session " +  this.sessionShellId.toString() + " has been created");
    }

    public ExpectedOutcome executeCommand(Command command) throws IOException{
        //In order to determine when our command has finished execution, echo the session identifier before and after the command output
        this.commandOrdinal++;
        String commandEnd = getCommandEndIdentifier();

        /*Write our command to the input stream, then writes our command identification buffer
        * Each command has a line break appended to instruct the bash terminal to execute the command
        * This implementation currently writes the command and then flushes it.
        * If writing an excessively long command (more than std_in buffer size) the buffer will fill before it flushes.
        * Keep your commands short*/
        this.commandLock.lock();
        logger.debug(this.getTerminalIdentifier() + " session acquired lock");
        processInput.write(command.getCommandText() + MessageLiterals.LineBreak);
        processInput.write("echo " + commandEnd + MessageLiterals.LineBreak);
        processInput.flush();

        //Wait for the process wrappers to finish writing the current command output
        try {
            Thread.sleep(command.getMinimalSecondsToResponse() * 1000);
            logger.debug(this.getTerminalIdentifier() + " session started command wait");
            this.commandOutputFinished.await((long)(command.getSecondsToTimeout() - command.getMinimalSecondsToResponse()), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Session " + this.sessionShellId.toString() + " interrupted while waiting for command " + this.commandOrdinal + "to return.", e);
        }
        logger.debug(this.getTerminalIdentifier() + " session finished command wait");

        //Clear the command identification buffers from the command output, and create an immutable copy to pass around
        //Then clear the current command output, and signal to the process wrappers that they may proceed to write the next command
        List<String> immutableOutput;
        boolean timeoutOccured;
        try {
            synchronized (this.commandOutput) {
                timeoutOccured = !this.commandOutput.remove(commandEnd); //the remove() method returns true if the end buffer existed before it was cleared
                immutableOutput = new ArrayList<>(Collections.unmodifiableList(this.commandOutput));
                this.commandOutput.clear();
            }
        }finally {
            this.commandLock.unlock();
            logger.debug(this.getTerminalIdentifier() + " session released lock");
        }

        /*Check if the command output matches any of our expected outcomes
         * If a match is found, return the corresponding result for that expected outcome.
         * If no expected outcome achieved (or none exist), return CommandResult.SUCCESS to progress to the next command*/
        if(timeoutOccured){
            //If a timeout occured, the command failed to execute and the method will return a failure
            return ExpectedOutcome.executionError(MessageLiterals.TimeoutInCommand);
        }else if(command.getExpectedOutcomes().isEmpty()){
            return ExpectedOutcome.defaultOutcome();
        }else {
            ExpectedOutcome resolvedOutcome = ExpectedOutcomeResolver.resolveExpectedOutcome(immutableOutput, command.getExpectedOutcomes(), command.getOutcomeAggregation());
            if(resolvedOutcome.weakEquals(ExpectedOutcome.defaultOutcome()) && command.getOutcomeAggregation().isAggregating()){
                resolvedOutcome.setMessage(command.getAggregatedOutcomeMessage());
            }
            return resolvedOutcome;
        }
    }

    String getTerminalIdentifier(){
        return this.sessionShellId.toString()+"-cmd-"+this.commandOrdinal;
    }

    String getCommandEndIdentifier(){
        return this.getTerminalIdentifier()+"-end";
    }

    Lock getCommandLock() {
        return commandLock;
    }

    Condition getCommandOutputFinished() {
        return commandOutputFinished;
    }

    public ProcessStreamWrapper getProcessOutputAndErrors() {
        return processOutputAndErrors;
    }

    @Override
    public void close() throws IOException {
        this.process.destroy();
        this.processInput.close();
        logger.info("Session " +  this.sessionShellId.toString() + " has been closed");
    }
}
