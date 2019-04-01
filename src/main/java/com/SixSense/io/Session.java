package com.SixSense.io;


import com.SixSense.data.commands.ICommand;
import com.SixSense.data.outcomes.CommandType;
import com.SixSense.data.commands.Command;
import com.SixSense.data.outcomes.ExpectedOutcome;
import com.SixSense.data.outcomes.ResultStatus;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.ExpectedOutcomeResolver;
import com.SixSense.util.MessageLiterals;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Session implements Closeable {
    private static Logger logger = Logger.getLogger(Session.class);

    private final Process localProcess; //The operating system process to which we write local commands
    private final Process remoteProcess; //The operating system process to which we write remote commands (ideally - after a connect block)
    private final List<String> commandOutput; //Line separated response from both the process output stream and the process error stream.
    private Lock commandLock =  new ReentrantLock();
    private final Condition commandOutputFinished = commandLock.newCondition();

    private final UUID sessionShellId = UUID.randomUUID();
    private int commandOrdinal = 0;

    private final BufferedWriter localProcessInput;
    private final BufferedWriter remoteProcessInput;
    private final ProcessStreamWrapper localOutputAndErrors;
    private final ProcessStreamWrapper remoteOutputAndErrors;

    private final Map<String, Deque<VariableRetention>> sessionVariables = new HashMap<>();


    public Session(Process localProcess, Process remoteProcess){
        this.localProcess = localProcess;
        this.remoteProcess = remoteProcess;
        this.commandOutput = new ArrayList<>();

        this.localProcessInput = new BufferedWriter(new OutputStreamWriter(localProcess.getOutputStream()));
        this.remoteProcessInput = new BufferedWriter(new OutputStreamWriter(remoteProcess.getOutputStream()));
        this.localOutputAndErrors = new ProcessStreamWrapper(localProcess.getInputStream(), this, this.commandOutput);
        this.remoteOutputAndErrors = new ProcessStreamWrapper(remoteProcess.getInputStream(), this, this.commandOutput);
        logger.info("Session " +  this.sessionShellId.toString() + " has been created");
    }


    public ExpectedOutcome executeCommand(Command command) throws IOException{
        if(command.getCommandType().equals(CommandType.LOCAL)){
            return executeCommand(command, localProcessInput);
        }else if(command.getCommandType().equals(CommandType.REMOTE)){
            return executeCommand(command, remoteProcessInput);
        }else{
            return ExpectedOutcome.executionError(MessageLiterals.InvalidCommandParameters);
        }
    }

    private ExpectedOutcome executeCommand(Command command, BufferedWriter writeToProcess) throws IOException{
        //In order to determine when our command has finished execution, echo the session identifier before and after the command output
        this.commandOrdinal++;
        String commandEnd = getCommandEndIdentifier();

        /*Write our command to the input stream, then writes our command identification buffer
        * Each command has a line break appended to instruct the bash terminal to execute the command
        * This implementation currently writes the command and then flushes it.
        * If writing an excessively long command (more than std_in buffer size) the buffer will fill before it flushes.
        * Keep your commands short*/
        this.commandLock.lock();
        //logger.debug(this.getTerminalIdentifier() + " session acquired lock");
        String evaluatedCommand = CommandUtils.evaluateAgainstDynamicFields(command, this.getCurrentSessionVariables());
        writeToProcess.write(evaluatedCommand + MessageLiterals.LineBreak);
        writeToProcess.write("echo " + commandEnd + MessageLiterals.LineBreak);
        writeToProcess.flush();

        //Wait for the process wrappers to finish writing the current command output
        try {
            Thread.sleep(command.getMinimalSecondsToResponse() * 1000);
            //logger.debug(this.getTerminalIdentifier() + " session started command wait");
            this.commandOutputFinished.await((long)(command.getSecondsToTimeout() - command.getMinimalSecondsToResponse()), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Session " + this.sessionShellId.toString() + " interrupted while waiting for command " + this.commandOrdinal + "to return.", e);
        }
        //logger.debug(this.getTerminalIdentifier() + " session finished command wait");

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
            //logger.debug(this.getTerminalIdentifier() + " session released lock");
        }

        /*Check if the command output matches any of our expected outcomes
         * If a match is found, return the corresponding result for that expected outcome.
         * If no expected outcome achieved (or none exist), return CommandResult.SUCCESS to progress to the next command*/
        ExpectedOutcome resolvedOutcome;
        if(timeoutOccured){
            //If a timeout occured, the command failed to execute and the method will return a failure
            resolvedOutcome = ExpectedOutcome.executionError(MessageLiterals.TimeoutInCommand);
        }else if(command.getExpectedOutcomes().isEmpty()){
            resolvedOutcome = ExpectedOutcome.defaultOutcome();
        }else {
            resolvedOutcome = ExpectedOutcomeResolver.resolveExpectedOutcome(immutableOutput, command.getExpectedOutcomes(), command.getOutcomeAggregation());
            if(resolvedOutcome.weakEquals(ExpectedOutcome.defaultOutcome()) && command.getOutcomeAggregation().isAggregating()){
                resolvedOutcome.setMessage(command.getAggregatedOutcomeMessage());
            }
        }

        if(resolvedOutcome.getOutcome().equals(ResultStatus.SUCCESS)){
            if(command.getSaveTo().getResultRetention().equals(ResultRetention.Variable)){
                this.sessionVariables.putIfAbsent(command.getSaveTo().getName(), new ArrayDeque<>());
                this.sessionVariables.get(command.getSaveTo().getName()).push(command.getSaveTo());
            }
        }

        return resolvedOutcome;
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

    public ProcessStreamWrapper getLocalOutputAndErrors() {
        return localOutputAndErrors;
    }

    public ProcessStreamWrapper getRemoteOutputAndErrors() {
        return remoteOutputAndErrors;
    }

    public void loadSessionVariables(Map<String, String> properties){
        for(String propertyName : properties.keySet()){
            this.sessionVariables.putIfAbsent(propertyName, new ArrayDeque<>());
            this.sessionVariables.get(propertyName).push(
                new VariableRetention()
                    .withName(properties.get(propertyName))
                    .withResultRetention(ResultRetention.Variable)
                    .withOverwriteParent(false)
            );
        }
    }

    public void loadSessionDynamicFields(ICommand context){
        Map<String, String> contextDynamicFields = context.getDynamicFields();
        loadSessionVariables(contextDynamicFields);
    }

    public void removeSessionDynamicFields(ICommand context){
        Map<String, String> contextDynamicFields = context.getDynamicFields();
        for(String propertyName : contextDynamicFields.keySet()){
            Deque<VariableRetention> dynamicFieldStack = this.sessionVariables.get(propertyName);
            VariableRetention topmostVariable = dynamicFieldStack.pop();

            if(topmostVariable.isOverwriteParent()){
                if(!dynamicFieldStack.isEmpty()) {
                    dynamicFieldStack.pop();
                }
                dynamicFieldStack.push(topmostVariable.withOverwriteParent(false));
            }
        }
    }

    private Map<String, String> getCurrentSessionVariables(){
        Map<String, String> currentSessionFields = new HashMap<>();
        for(String field : this.sessionVariables.keySet()){
            VariableRetention currentValue = this.sessionVariables.get(field).peek();
            if(currentValue != null) {
                currentSessionFields.put(field, currentValue.getName());
            }
        }
        return currentSessionFields;
    }

    @Override
    public void close() throws IOException {
        this.localProcess.destroy();
        this.remoteProcess.destroy();
        this.localProcessInput.close();
        this.remoteProcessInput.close();
        logger.info("Session " +  this.sessionShellId.toString() + " has been closed");
    }
}
