package com.SixSense.io;


import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Command;
import com.SixSense.data.events.InputSentEvent;
import com.SixSense.data.events.OutcomeEvaluationEvent;
import com.SixSense.data.events.OutputReceivedEvent;
import com.SixSense.data.events.ResultRetentionEvent;
import com.SixSense.data.logging.Loggers;
import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.data.logic.ResultStatus;
import com.SixSense.data.retention.RetentionType;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.engine.DiagnosticManager;
import com.SixSense.queue.WorkerQueue;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.LogicalExpressionResolver;
import com.SixSense.util.MessageLiterals;
import net.schmizz.sshj.SSHClient;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;

public class Session implements Closeable{
    //Static members and injected beans
    private static final Logger sessionLogger = LogManager.getLogger(Loggers.SessionLogger.name());
    @Autowired private WorkerQueue workerQueue;
    @Autowired private DiagnosticManager diagnosticManager;

    //Connection, synchronization and debugging
    private final Map<String, ShellChannel> channels;
    private final Lock commandLock =  new ReentrantLock();
    private final Condition newChunkReceived = commandLock.newCondition();
    private boolean isUnderDebug = false;
    private boolean isClosed = false;

    //Current command context
    private final UUID sessionShellId = UUID.randomUUID();
    private int drilldownRank = 0;
    private Command currentCommand;
    private int commandOrdinal = 0;
    private String evaluatedCommand = "";
    private String currentPrompt = "";

    //Dynamic fields
    private final Map<String, Deque<ResultRetention>> sessionVariables;
    private final Map<String, String> databaseVariables;

    public Session(SSHClient connectedSSHClient, Set<String> channelNames) throws IOException{
        this.sessionVariables = new HashMap<>();
        this.databaseVariables = new HashMap<>();
        this.channels = new HashMap<>();
        for(String channelName : channelNames){
            ShellChannel newChannel = new ShellChannel(channelName, connectedSSHClient, this);
            this.channels.put(channelName, newChannel);
        }

        //Logging configurations
        this.loadSessionVariables(Collections.singletonMap("sixsense.session.workingDir", MessageLiterals.SessionExecutionDir + this.getSessionShellId()));
    }

    public ExpressionResult executeCommand(Command command) throws IOException{
        ShellChannel channel = this.channels.get(command.getChannelName());
        if(channel == null){
            return ExpressionResult.executionError(MessageLiterals.InvalidCommandParameters);
        }else{
            String promptReference = this.getPromptReference(channel.getName().toLowerCase());
            String nonFinalPrompt = this.getSessionVariableValue(promptReference);
            if(nonFinalPrompt == null || nonFinalPrompt.isEmpty()){
                String defaultPromptReference = this.getPromptReference("default");
                nonFinalPrompt = this.getSessionVariableValue(defaultPromptReference);
                this.loadSessionVariables(Collections.singletonMap(promptReference, nonFinalPrompt));
            }

            this.currentPrompt = nonFinalPrompt;
            return executeCommand(command, channel);
        }
    }

    private ExpressionResult executeCommand(Command command, ShellChannel channel) throws IOException {
        this.commandOrdinal++;
        this.currentCommand = command;
        this.evaluatedCommand = CommandUtils.evaluateAgainstDynamicFields(command.getCommandText(), this.getCurrentSessionVariables());
        final List<String> processOutput = channel.getChannelOutput();

        /*Write our current command to the input stream,
        * Each command has a line break character appended to instruct the bash terminal to execute the command
        * This implementation currently writes the command and then flushes it.
        * If writing an excessively long command (more than std_in buffer size) the buffer will fill before it flushes.
        * Keep your commands short*/
        this.commandLock.lock();
        channel.write(this.evaluatedCommand + MessageLiterals.LineBreak);
        channel.flush();
        diagnosticManager.emit(new InputSentEvent(this, command, this.commandOrdinal, this.evaluatedCommand));

        String output = "";
        long elapsedSeconds = 0L;
        boolean commandEndReached = false;
        boolean hasWaitElapsed = command.getExpectedOutcome().getResolvableExpressions().isEmpty();
        ExpressionResult resolvedOutcome = ExpressionResult.defaultOutcome();
        LocalDateTime commandStartTime = LocalDateTime.now();
        try {
            Thread.sleep(command.getMinimalSecondsToResponse() * 1000);
            while(!hasWaitElapsed){
                /*this.removeOutdatedChunks() clears the command output from data left over from previous commands (edits processOutput in place)
                 *and returns a boolean which is true only if the command has certainly finished writing it's output (if true, then certainly finished. if false, may be either way)
                 *CommandUtils.pipeCommandOutput() passes the output through any pipes defined by this command, possibly mutating, replacing or truncating it. */
                List<String> pipedProcessOutput;
                synchronized (processOutput) {
                    commandEndReached = this.removeOutdatedChunks(processOutput);
                    pipedProcessOutput = new ArrayList<>(CommandUtils.pipeCommandOutput(this, processOutput));
                }

                //Parse the command output into a concatenated user-friendly string
                if(command.isUseRawOutput()) {
                    output = String.join(MessageLiterals.LineBreak, pipedProcessOutput);
                }else {
                    output = this.filterRawOutput(pipedProcessOutput);
                }

                /*Attempt to resolve the latest chunk against the current expected logic
                 * Then continue if successful, if the command returned completely, or if our waiting period had elapsed */
                resolvedOutcome = this.attemptToResolve(command, output);
                elapsedSeconds = commandStartTime.until(LocalDateTime.now(), ChronoUnit.SECONDS);
                if(commandEndReached || resolvedOutcome.isResolved() || elapsedSeconds >= command.getSecondsToTimeout() - command.getMinimalSecondsToResponse()){
                    hasWaitElapsed = true;
                }else {
                    this.newChunkReceived.await(command.getSecondsToTimeout() - elapsedSeconds, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException e) {
            sessionLogger.warn(MessageLiterals.Tab + "Session " + this.getShortSessionId() + " interrupted while waiting for command " + this.commandOrdinal + " to return", e);
        }
        diagnosticManager.emit(new OutputReceivedEvent(this, command, this.commandOrdinal, output));
        diagnosticManager.emit(new OutcomeEvaluationEvent(this, output, command.getExpectedOutcome()));
        this.commandLock.unlock();

        //If the command has been resolved, check if the result should be retained in any way, and save it if necessary
        if(resolvedOutcome.getOutcome().equals(ResultStatus.SUCCESS)){
            //We clone the retention so that if the command is called again, any action we take within this code block will not affect subsequent executions
            ResultRetention clonedRetention = command.getSaveTo().deepClone();
            if(clonedRetention.getValue().isEmpty()){
                clonedRetention.setValue(CommandUtils.pipeCommandRetention(this, output));
            }else{
                clonedRetention.setValue(CommandUtils.pipeCommandRetention(this, clonedRetention.getValue()));
            }

            diagnosticManager.emit(new ResultRetentionEvent(this, clonedRetention));
            if(clonedRetention.getRetentionType().equals(RetentionType.Variable)){
                String variable = clonedRetention.getName();
                this.sessionVariables.putIfAbsent(variable, new ArrayDeque<>());
                Deque<ResultRetention> varStack = this.sessionVariables.get(variable);
                if(!varStack.isEmpty()) {
                    varStack.pop();
                }
                varStack.push(clonedRetention);
            }else if(clonedRetention.getRetentionType().equals(RetentionType.File)){
                clonedRetention.setValue(this.filterFileOutput(clonedRetention.getValue()));
                RetentionFileWriter fileWriter = new RetentionFileWriter(this.getSessionShellId(), clonedRetention.getName(), clonedRetention.getValue());
                try {
                    this.workerQueue.submit(fileWriter);
                } catch (Exception e) {
                    sessionLogger.error("Failed to save file " + clonedRetention.getName() + " to file system", e);
                }
            }else if(clonedRetention.getRetentionType().equals(RetentionType.Database)){
                //TODO: once we provide a database service, this should write the key value pairs async, similar to file retention.
                this.databaseVariables.put(clonedRetention.getName(), clonedRetention.getValue());
            }
        }else if(resolvedOutcome.getMessage().equals(MessageLiterals.CommandDidNotReachOutcome) && elapsedSeconds >= command.getSecondsToTimeout()){
            //If a timeout occured, the command failed to execute and the method will return a failure
            resolvedOutcome.setMessage(MessageLiterals.TimeoutInCommand);
        }

        /*Perform a cleanup on the process output if a cleanup is required (by default or if commandEndReached is true)
        * If commandEndReached is true, the last line is the current prompt; we can safely remove all preceding lines
         * If a cleanup is required, we have no guarantee the last line is not being edited; but we can still safely remove all preceding lines*/
        if (command.isRequiresCleanup() || commandEndReached) {
            synchronized (processOutput) {
                int cleanupCounter = processOutput.size() - 1; //all lines before this index (zero-based) will be cleared
                while (cleanupCounter > 0) {
                    processOutput.remove(0);
                    cleanupCounter--;
                }
            }
        }

        return resolvedOutcome;
    }

    private boolean removeOutdatedChunks(List<String> output){
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
        if(this.evaluatedCommand.contains("\n")){
            firstLineOfCommand = this.evaluatedCommand.substring(0, this.evaluatedCommand.indexOf("\n")+1); //if the command contains multiple lines, only search for the first line in the output
        }else{
            firstLineOfCommand = this.evaluatedCommand;
        }

        int firstRelevantIdx = 0; //all lines before this index (zero-based) will be cleared
        int promptScore = 0;
        int cmdScore = 0;
        boolean promptAppearsTwice = false;
        boolean commandAppearsOnce = false;
        for (int lineNum = output.size() - 1; lineNum >= 0 && promptScore + cmdScore < 2; lineNum--) {
            String currentLine = output.get(lineNum);
            if(currentLine.startsWith(this.currentPrompt)){
                promptScore++;
                firstRelevantIdx = lineNum;
                if(currentLine.contains(firstLineOfCommand)){
                    commandAppearsOnce = true;
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

        return commandAppearsOnce && promptAppearsTwice; //We assume that if the prompt appears twice, and the command appears once, than case 1) is met
    }

    /*We could theoretically just apply a ClearingPipe and then a WhitespacePipe
    * But then changes to the pipes could affect the session filtering methods*/
    private String filterRawOutput(List<String> output){
        StringJoiner stringRepresentation = new StringJoiner(" ", "", "");
        for(String line : output){
            stringRepresentation.add(line
                    .replace(MessageLiterals.CarriageReturn+MessageLiterals.LineBreak, " ")
                    .replace(MessageLiterals.LineBreak, " ")
                    .replace(MessageLiterals.CarriageReturn, " ")
                    .replace(this.evaluatedCommand, "")
            );
        }

        return stringRepresentation.toString()
                .replaceAll("\\s+", " ")
                .replace(this.evaluatedCommand, "")
                .replace(this.currentPrompt, "");
    }

    /*We could theoretically just apply a ClearingPipe and then a WhitespacePipe
     * But then changes to the pipes could affect the session filtering methods*/
    private String filterFileOutput(String fileData){
        return fileData
                .replace(this.evaluatedCommand, "")
                .replace(this.currentPrompt, "");
    }

    private ExpressionResult attemptToResolve(Command command, String outputAsString){
        /*Check if the command output matches any of our expected logic
         * If a match is found, return the corresponding result for that expected outcome.
         * If no expected outcome achieved (or none exist), return CommandResult.SUCCESS to progress to the next command*/
        return LogicalExpressionResolver.resolveLogicalExpression(
                outputAsString,
                this.getCurrentSessionVariables(),
                command.getExpectedOutcome()
        );
    }

    public String getSessionShellId() {
        return this.sessionShellId.toString();
    }

    public String getShortSessionId() {
        return this.sessionShellId.toString().substring(0,8);
    }

    public int getDrilldownRank(){
        return drilldownRank;
    }

    public void incrementDrilldownRank(){
        drilldownRank++;
    }

    public void decrementDrilldownRank(){
        drilldownRank--;
    }

    public String getTerminalIdentifier(){
        return this.getShortSessionId()+"-cmd-"+this.commandOrdinal;
    }

    public Command getCurrentCommand(){
        return this.currentCommand;
    }

    public String getCurrentEvaluatedCommand(){
        return this.evaluatedCommand;
    }

    public String getCurrentPrompt(){
        return this.currentPrompt;
    }

    private String getPromptReference(String channelName){
        return "sixsense.session.prompt."+channelName;
    }

    Lock getCommandLock() {
        return commandLock;
    }

    Condition getNewChunkReceived() {
        return newChunkReceived;
    }

    public Map<String, ShellChannel> getShellChannels() {
        return Collections.unmodifiableMap(this.channels);
    }

    public void loadSessionVariables(Map<String, String> properties){
        for(String propertyName : properties.keySet()){
            this.sessionVariables.putIfAbsent(propertyName, new ArrayDeque<>());
            this.sessionVariables.get(propertyName).push(
                new ResultRetention()
                    .withName(propertyName)
                    .withValue(properties.get(propertyName))
                    .withRetentionType(RetentionType.Variable)
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
            Deque<ResultRetention> dynamicFieldStack = this.sessionVariables.get(propertyName);
            ResultRetention topmostVariable = dynamicFieldStack.pop();

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
            ResultRetention currentValue = this.sessionVariables.get(field).peek();
            if(currentValue != null) {
                currentSessionFields.put(field, currentValue.getValue());
            }
        }
        return currentSessionFields;
    }

    private String getSessionVariableValue(String sessionVar){
        if(this.sessionVariables.containsKey(sessionVar) && !this.sessionVariables.get(sessionVar).isEmpty()){
            ResultRetention latestValue = this.sessionVariables.get(sessionVar).peek();
            if(latestValue != null) {
                return latestValue.getValue();
            }
        }
        return "";
    }

    public Map<String, String> getDatabaseVariables(){
        return Collections.unmodifiableMap(this.databaseVariables);
    }

    public boolean isUnderDebug() {
        return isUnderDebug;
    }

    public void activateDebugMode() {
        for(ShellChannel channel : getShellChannels().values()){
            channel.activateDebugMode();
        }
        isUnderDebug = true;
    }

    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void close() throws IOException{
        boolean partialClosure = false;
        for(String channelName : this.channels.keySet()){
            //Try to close each channel in it's own try block, so failure in one channel will not affect other channels
            try {
                this.channels.get(channelName).close();
            }catch (IOException e){
                partialClosure = true;
                sessionLogger.error("Session " +  this.getShortSessionId() + " failed to close channel with name " + channelName, e);
            }
        }
        this.isClosed = true;

        if(partialClosure){
            throw new IOException("Session " +  this.getShortSessionId() + " failed to close one or more of it's channels ");
        }
    }
}
