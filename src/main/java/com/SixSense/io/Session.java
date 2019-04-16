package com.SixSense.io;


import com.SixSense.data.commands.ICommand;
import com.SixSense.data.logic.CommandType;
import com.SixSense.data.commands.Command;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.ResultStatus;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.ExpectedOutcomeResolver;
import com.SixSense.util.MessageLiterals;
import org.apache.log4j.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import net.schmizz.sshj.connection.channel.direct.Session.Shell;

public class Session implements Closeable {
    private static Logger logger = Logger.getLogger(Session.class);

    private final net.schmizz.sshj.connection.channel.direct.Session localSession;
    private final net.schmizz.sshj.connection.channel.direct.Session remoteSession;
    private final Shell localShell; //The operating system process to which we write local commands
    private final Shell remoteShell; //The operating system process to which we write remote commands (ideally - after a connect block)
    private final List<String> localOutput; //Line separated response from both the local process output stream and the process error stream.
    private final List<String> remoteOutput; //Line separated response from both the remote process output stream and the process error stream.
    private Lock commandLock =  new ReentrantLock();
    private final Condition newChunkReceived = commandLock.newCondition();

    private boolean isClosed = false;
    private final UUID sessionShellId = UUID.randomUUID();
    private int commandOrdinal = 0;
    private String currentPrompt;

    private final BufferedWriter localProcessInput;
    private final BufferedWriter remoteProcessInput;
    private final ProcessStreamWrapper localStreamWrapper;
    private final ProcessStreamWrapper remoteStreamWrapper;

    private final Map<String, Deque<VariableRetention>> sessionVariables = new HashMap<>();


    public Session(net.schmizz.sshj.connection.channel.direct.Session localSession, net.schmizz.sshj.connection.channel.direct.Session remoteSession) throws IOException{
        this.localSession = localSession;
        this.remoteSession = remoteSession;
        this.localSession.allocateDefaultPTY();
        this.remoteSession.allocateDefaultPTY();
        this.localShell = localSession.startShell();
        this.remoteShell = remoteSession.startShell();

        this.localOutput = new ArrayList<>();
        this.remoteOutput = new ArrayList<>();

        this.localProcessInput = new BufferedWriter(new OutputStreamWriter(this.localShell.getOutputStream()));
        this.remoteProcessInput = new BufferedWriter(new OutputStreamWriter(this.remoteShell.getOutputStream()));
        this.localStreamWrapper = new ProcessStreamWrapper(this.localShell.getInputStream(), this, this.localOutput);
        this.remoteStreamWrapper = new ProcessStreamWrapper(this.remoteShell.getInputStream(), this, this.remoteOutput);

        logger.info("Session " +  this.sessionShellId.toString() + " has been created");
    }

    public ExpectedOutcome executeCommand(Command command) throws IOException{
        if(command.getCommandType().equals(CommandType.LOCAL)){
            this.currentPrompt = this.getSessionVariableValue("sixsense.session.localPrompt");
            return executeCommand(command, localProcessInput, localOutput);
        }else if(command.getCommandType().equals(CommandType.REMOTE)){
            this.currentPrompt = this.getSessionVariableValue("sixsense.session.remotePrompt");
            return executeCommand(command, remoteProcessInput, remoteOutput);
        }else{
            return ExpectedOutcome.executionError(MessageLiterals.InvalidCommandParameters);
        }
    }

    private ExpectedOutcome executeCommand(Command command, final BufferedWriter writeToProcess, final List<String> processOutput) throws IOException{
        //In order to determine when our command has finished execution, echo the session identifier before and after the command output
        this.commandOrdinal++;
        String evaluatedCommand = CommandUtils.evaluateAgainstDynamicFields(command, this.getCurrentSessionVariables());

        /*Write a command identification prompt, and then write our current command to the input stream,
        * Each command has a line break appended to instruct the bash terminal to execute the command
        * This implementation currently writes the command and then flushes it.
        * If writing an excessively long command (more than std_in buffer size) the buffer will fill before it flushes.
        * Keep your commands short*/
        this.commandLock.lock();
        logger.debug(this.getTerminalIdentifier() + " session acquired lock");
        logger.info(this.getSessionShellId() + " is writing " + command.getCommandType() +" command " + evaluatedCommand);
        writeToProcess.write(evaluatedCommand + MessageLiterals.LineBreak);
        writeToProcess.flush();

        String output = "";
        long elapsedSeconds = 0L;
        boolean commandEndReached;
        boolean hasWaitElapsed = command.getExpectedOutcomes().isEmpty();
        ExpectedOutcome resolvedOutcome = ExpectedOutcome.defaultOutcome();
        LocalDateTime commandStartTime = LocalDateTime.now();
        try {
            Thread.sleep(command.getMinimalSecondsToResponse() * 1000);
            while(!hasWaitElapsed){
                /*this.removeOutdatedChunks() clears the command output from data left over from previous commands (edits processOutput in place)
                 *and returns a boolean which is true only if the command has certainly finished writing it's output (if true, then certainly finished. if false, may be either way)*/
                List<String> pipedProcessOutput;
                synchronized (processOutput) {
                    commandEndReached = this.removeOutdatedChunks(evaluatedCommand, processOutput);
                    pipedProcessOutput = CommandUtils.pipeCommandOutput(command, processOutput);
                    if(commandEndReached){
                        processOutput.clear();
                        processOutput.add(this.currentPrompt);
                    }
                }

                //Parse the command output into a concatenated user-friendly string
                if(command.isUseRawOutput()) {
                    output = String.join(MessageLiterals.LineBreak, pipedProcessOutput);
                }else {
                    output = this.filterRawOutput(evaluatedCommand, pipedProcessOutput);
                }

                /*Attempt to resolve the latest chunk against the current expected logic
                * Then continue if successful, if the command returned completely, or if our waiting period had elapsed */
                resolvedOutcome = this.attemptToResolve(command, output);
                elapsedSeconds = commandStartTime.until(LocalDateTime.now(), ChronoUnit.SECONDS);
                if(commandEndReached || resolvedOutcome.isResolved() || elapsedSeconds >= command.getSecondsToTimeout() - command.getMinimalSecondsToResponse()){
                    hasWaitElapsed = true;
                }else {
                    this.newChunkReceived.await((long) (command.getSecondsToTimeout() - elapsedSeconds), TimeUnit.SECONDS);
                }
            }

        } catch (InterruptedException e) {
            logger.warn("Session " + this.sessionShellId.toString() + " interrupted while waiting for command " + this.commandOrdinal + "to return.", e);
        }
        logger.debug(this.getTerminalIdentifier() + " session finished command wait");
        logger.info("command output was " + output);
        logger.info("resolved outcome is " + resolvedOutcome);
        this.commandLock.unlock();
        logger.debug(this.getTerminalIdentifier() + " session released lock");

        //If the command has been resolved, check if the result should be retained in any way, and save it if necessary
        if(resolvedOutcome.getOutcome().equals(ResultStatus.SUCCESS)){
            if(command.getSaveTo().getValue().isEmpty()){
                command.getSaveTo().setValue(output);
            }
            if(command.getSaveTo().getResultRetention().equals(ResultRetention.Variable)){
                String variable = command.getSaveTo().getName();
                this.sessionVariables.putIfAbsent(variable, new ArrayDeque<>());
                Deque<VariableRetention> varStack = this.sessionVariables.get(variable);
                if(!varStack.isEmpty()) {
                    varStack.pop();
                }
                varStack.push(command.getSaveTo());
            }
        }else if(resolvedOutcome.getMessage().equals(MessageLiterals.CommandDidNotReachOutcome) && elapsedSeconds >= command.getSecondsToTimeout()){
            //If a timeout occured, the command failed to execute and the method will return a failure
            resolvedOutcome.setMessage(MessageLiterals.TimeoutInCommand);
        }

        return resolvedOutcome;
    }

    private boolean removeOutdatedChunks(String evaluatedCommand, List<String> output){
        /*This method assumes we are holding the synchronized block for the (List<String> output) in question

        * We scan for the current prompt and the last evaluated command in the output
        * and return the latest chunk that satisfies either of the following conditions:
        * 1) The chunk lies between the two most recent occurrences of the current prompt
        *       (in which case, the process stream has read the full output of the command)
        * 2) The chunk is preceded by the current command, which is in turn preceeded by the current prompt
        *       (in which case, the process stream has either read part of the command, or the command is not followed by the current prompt
        *
        * Then we clear the output of all outdated chunks,
        * and return if the current chunk lies between two occurrences of the current prompt*/

        if(output.isEmpty()){
            return false;
        }

        String firstLineOfCommand;
        if(evaluatedCommand.contains("\n")){
            firstLineOfCommand = evaluatedCommand.substring(0, evaluatedCommand.indexOf("\n")+1); //if the command contains multiple lines, only search for the first line in the output
        }else{
            firstLineOfCommand = evaluatedCommand;
        }

        int firstRelevantIdx = 0;
        int promptScore = 0;
        int cmdScore = 0;
        boolean promptAppearsTwice = false;
        for (int lineNum = output.size() - 1; lineNum >= 0 && promptScore + cmdScore < 2; lineNum--) {
            String currentLine = output.get(lineNum);
            if(currentLine.startsWith(this.currentPrompt)){
                promptScore++;
                firstRelevantIdx = lineNum;
                if(currentLine.contains(firstLineOfCommand)){
                    cmdScore++;
                }
                if(promptScore >= 2){
                    promptAppearsTwice = true;
                }
            }
        }

        while(firstRelevantIdx > 0){
            output.remove(0);
            firstRelevantIdx--;
        }

        return promptAppearsTwice; //We assume that if the prompt appears twice, than case 1) is met
    }

    private String filterRawOutput(String evaluatedCommand, List<String> output){
        StringJoiner stringRepresentation = new StringJoiner(" ", "", "");
        for(String line : output){
            stringRepresentation.add(line
                    .replace(MessageLiterals.CarriageReturn+MessageLiterals.LineBreak, " ")
                    .replace(MessageLiterals.LineBreak, " ")
                    .replace(MessageLiterals.CarriageReturn, " ")
                    .replace(evaluatedCommand, "")
                    .trim()
            );
        }

        return stringRepresentation.toString()
                .replaceAll("\\s+", " ")
                .replace(evaluatedCommand, "")
                .replace(this.currentPrompt, "")
                .trim();
    }

    private ExpectedOutcome attemptToResolve(Command command, String outputAsString){
        /*Check if the command output matches any of our expected logic
         * If a match is found, return the corresponding result for that expected outcome.
         * If no expected outcome achieved (or none exist), return CommandResult.SUCCESS to progress to the next command*/
        ExpectedOutcome resolvedOutcome = ExpectedOutcomeResolver.resolveExpectedOutcome(
                outputAsString,
                this.getCurrentSessionVariables(),
                command.getExpectedOutcomes(),
                command.getOutcomeAggregation()
        );
        if(resolvedOutcome.weakEquals(ExpectedOutcome.defaultOutcome()) && command.getOutcomeAggregation().isAggregating()){
            resolvedOutcome.setMessage(command.getAggregatedOutcomeMessage());
        }

        return resolvedOutcome;
    }

    public String getSessionShellId() {
        return this.sessionShellId.toString();
    }

    String getTerminalIdentifier(){
        return this.getSessionShellId()+"-cmd-"+this.commandOrdinal;
    }

    Lock getCommandLock() {
        return commandLock;
    }

    Condition getNewChunkReceived() {
        return newChunkReceived;
    }

    public ProcessStreamWrapper getLocalStreamWrapper() {
        return localStreamWrapper;
    }

    public ProcessStreamWrapper getRemoteStreamWrapper() {
        return remoteStreamWrapper;
    }

    public void loadSessionVariables(Map<String, String> properties){
        for(String propertyName : properties.keySet()){
            this.sessionVariables.putIfAbsent(propertyName, new ArrayDeque<>());
            this.sessionVariables.get(propertyName).push(
                new VariableRetention()
                    .withName(propertyName)
                    .withValue(properties.get(propertyName))
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

    public Map<String, String> getCurrentSessionVariables(){
        Map<String, String> currentSessionFields = new HashMap<>();
        for(String field : this.sessionVariables.keySet()){
            VariableRetention currentValue = this.sessionVariables.get(field).peek();
            if(currentValue != null) {
                currentSessionFields.put(field, currentValue.getValue());
            }
        }
        return currentSessionFields;
    }

    private String getSessionVariableValue(String sessionVar){
        if(this.sessionVariables.containsKey(sessionVar) && !this.sessionVariables.get(sessionVar).isEmpty()){
            return this.sessionVariables.get(sessionVar).peek().getValue();
        }
        return "";
    }

    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void close() throws IOException {
        this.localShell.close();
        this.remoteShell.close();
        this.localSession.close();
        this.remoteSession.close();
        this.localProcessInput.close();
        this.remoteProcessInput.close();
        this.isClosed = true;
        logger.info("Session " +  this.sessionShellId.toString() + " has been closed");
    }
}
