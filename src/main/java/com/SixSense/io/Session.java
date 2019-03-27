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
    private final Condition commandOutputClear = commandLock.newCondition();
    private final Condition commandOutputFinished = commandLock.newCondition();

    private final UUID sessionShellId = UUID.randomUUID();
    private int commandOrdinal = 0;

    private final BufferedWriter processInput;
    private final ProcessStreamWrapper processOutput;
    private final ProcessStreamWrapper processErrors;


    public Session(Process process){
        this.process = process;
        this.commandOutput = new ArrayList<>();

        this.processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        this.processOutput = new ProcessStreamWrapper(process.getInputStream(), this, this.commandOutput);
        this.processErrors = new ProcessStreamWrapper(process.getErrorStream(), this, this.commandOutput);
    }

    public ExpectedOutcome executeCommand(Command command) throws IOException{
        //In order to determine when our command has finished execution, echo the session identifier before and after the command output
        this.commandOrdinal++;
        String commandStart = getCommandStartIdentifier(this.commandOrdinal);
        String commandEnd = getCommandEndIdentifier(this.commandOrdinal);

        //Write our command to the input stream, surrounded by our command identification buffers
        //This implementation currently writes the command and then flushes it.
        //If writing an excessively long command (more than std_in buffer size) the buffer will fill before it flushes.
        //Keep your commands short
        processInput.write("echo " + commandStart + ";");
        processInput.write(command.getCommandText());
        processInput.write("echo " + commandEnd + ";");
        processInput.flush();

        //Wait for the process wrappers to finish writing the current command output
        this.commandLock.lock();
        try {
            Thread.sleep(command.getMinimalSecondsToResponse() * 1000);
            this.commandOutputFinished.await((long)(command.getSecondsToTimeout() - command.getMinimalSecondsToResponse()), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Session " + this.sessionShellId.toString() + " interrupted while waiting for command " + this.commandOrdinal + "to return.", e);
        }

        //clear the command identification buffers from the command output, and create an immutable copy to pass around
        ExpectedOutcome commandResult;
        List<String> immutableOutput;
        boolean timeoutOccured;
        synchronized (this.commandOutput) {
            this.commandOutput.remove(commandStart);
            timeoutOccured = !this.commandOutput.remove(commandEnd); //the remove() method returns true if the end buffer existed before it was cleared
            immutableOutput = Collections.unmodifiableList(this.commandOutput);
        }

        if(timeoutOccured){
            //If a timeout occured, the command failed to execute and the method will return a failure
            commandResult = ExpectedOutcome.executionError(MessageLiterals.TimeoutInCommand);
        }else{
            //Check if the command output matches any of our expected outcomes
            //If a match is found, return the corresponding result for that expected outcome.
            //If no expected outcome achieved (or none exist), return CommandResult.SUCCESS to progress to the next command
            commandResult = ExpectedOutcome.defaultOutcome();
            for(ExpectedOutcome possibleOutcome : command.getExpectedOutcomes()){
                ExpectedOutcome resolvedOutcome = ExpectedOutcomeResolver.ResolveExpectedOutcome(immutableOutput, possibleOutcome);
                if(resolvedOutcome.isResolved()){
                    commandResult = resolvedOutcome;
                    break;
                }
            }
        }

        //Clear the current command output, and signal to the process wrappers that they may proceed to write the next command
        synchronized (this.commandOutput) {
            this.commandOutput.clear();
        }
        this.commandOutputClear.signalAll();
        this.commandLock.unlock();

        return commandResult;
    }

    public UUID getSessionShellId() {
        return sessionShellId;
    }

    public String generateTerminalIdentifier(int commandOrdinal){
        return this.sessionShellId.toString()+"-cmd-"+commandOrdinal;
    }

    public String getCommandStartIdentifier(int commandOrdinal){
        return this.generateTerminalIdentifier(commandOrdinal)+"-start";
    }

    public String getCommandEndIdentifier(int commandOrdinal){
        return this.generateTerminalIdentifier(commandOrdinal)+"-end";
    }

    public int getCommandOrdinal() {
        return commandOrdinal;
    }

    public Lock getCommandLock() {
        return commandLock;
    }

    public Condition getCommandOutputClear() {
        return commandOutputClear;
    }

    public Condition getCommandOutputFinished() {
        return commandOutputFinished;
    }

    public ProcessStreamWrapper getProcessOutput() {
        return processOutput;
    }

    public ProcessStreamWrapper getProcessErrors() {
        return processErrors;
    }

    @Override
    public void close() throws IOException {
        processInput.close();
    }
}
