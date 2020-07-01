package com.sixsense.io;


import com.sixsense.api.amqp.OperationProducer;
import com.sixsense.model.commands.ICommand;
import com.sixsense.model.commands.Command;
import com.sixsense.model.events.InputSentEvent;
import com.sixsense.model.events.OutcomeEvaluationEvent;
import com.sixsense.model.events.OutputReceivedEvent;
import com.sixsense.model.events.ResultRetentionEvent;
import com.sixsense.model.logging.IDebuggable;
import com.sixsense.model.logging.Loggers;
import com.sixsense.model.logic.ExpressionResult;
import com.sixsense.model.logic.ResultStatus;
import com.sixsense.model.retention.DataType;
import com.sixsense.model.retention.DatabaseVariable;
import com.sixsense.model.retention.RetentionMode;
import com.sixsense.model.retention.ResultRetention;
import com.sixsense.services.DiagnosticManager;
import com.sixsense.threading.ThreadingManager;
import com.sixsense.utillity.CommandUtils;
import com.sixsense.utillity.LogicalExpressionResolver;
import com.sixsense.utillity.Literals;
import net.schmizz.sshj.SSHClient;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;

public class Session implements Closeable, IDebuggable {
    //Static members and injected beans
    private static final Logger sessionLogger = LogManager.getLogger(Loggers.SessionLogger.name());
    @Autowired private ThreadingManager threadingManager;
    @Autowired private DiagnosticManager diagnosticManager;
    @Autowired private OperationProducer operationProducer;

    //Connection, synchronization and debugging
    private final Map<String, ShellChannel> channels;
    private final Lock commandLock =  new ReentrantLock();
    private final Condition minimalSleepTerminated = commandLock.newCondition();
    private final Condition newChunkReceived = commandLock.newCondition();
    private boolean isUnderDebug = false;
    private boolean isClosed = false;
    private boolean terminatedExternally = false;

    //Current command context
    private final UUID sessionShellId = UUID.randomUUID();
    private final String operationId;
    private LocalDateTime commandStartTime;
    private long elapsedSeconds = 0;
    private int drilldownRank = 0;
    private Command currentCommand;
    private int commandOrdinal = 0;
    private String evaluatedCommand = "";
    private String currentPrompt = "";

    //Dynamic fields
    private final Map<String, Deque<ResultRetention>> sessionVariables;
    private final Set<DatabaseVariable> databaseVariables;

    public Session(SSHClient connectedSSHClient, Set<String> channelNames, String operationId) throws IOException{
        this.sessionVariables = new HashMap<>();
        this.databaseVariables = new HashSet<>();
        this.channels = new HashMap<>();
        for(String channelName : channelNames){
            ShellChannel newChannel = new ShellChannel(channelName, connectedSSHClient, this);
            this.channels.put(channelName, newChannel);
        }

        //Logging configurations
        this.operationId = operationId;
        this.loadSessionVariables(Collections.singletonMap("sixsense.session.workingDir", Literals.SessionExecutionDir + "/" + this.getShortSessionId()));
    }

    /*Extract the data needed to execute the command with the correct channel and prompt
    * (and then of course use them to execute the command)*/
    public ExpressionResult executeCommand(Command command) throws IOException{
        ShellChannel channel = this.channels.get(command.getChannelName());
        if(channel == null){
            return ExpressionResult.executionError(Literals.InvalidCommandParameters);
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
        /*Initialize local variables needed for executing the current command*/

        //output
        final List<String> processOutput = channel.getChannelOutput(); //The structured output from the process wrapped by the ProcessStreamWrapper for the current channel
        String parsedOutput = ""; //The string representation of the process output, parsed by this session for the current command

        //Halting conditions and elapsed time
        boolean commandEndReached = false;
        boolean hasWaitElapsed = command.getExpectedOutcome().getResolvableExpressions().isEmpty(); //ordinarily false, unless we do not wait for any results from the current command

        //Outcome evaluation
        ExpressionResult resolvedOutcome = ExpressionResult.defaultOutcome();

        /*And now the fun begins...*/
        assignContextVariables(command);
        this.commandLock.lock();
        writeCommand(channel);

        sleepMinimalSecondsToResponse();
        while(!hasWaitElapsed && !terminatedExternally){
            /*this.removeOutdatedChunks() clears the command output from data left over from previous commands (edits processOutput in place)
             *and returns a boolean which is true only if the command has certainly finished writing it's output (if true, then certainly finished. if false, may be either way)
             *CommandUtils.pipeCommandOutput() passes the output through any pipes defined by this command, possibly mutating, replacing or truncating it. */
            List<String> pipedProcessOutput;
            synchronized (processOutput) {
                commandEndReached = this.removeOutdatedChunks(processOutput);
                pipedProcessOutput = new ArrayList<>(CommandUtils.pipeCommandOutput(this, processOutput));
            }

            parsedOutput = parsePipedOutput(pipedProcessOutput);
            resolvedOutcome = attemptToResolve(parsedOutput);
            hasWaitElapsed = awaitIfNeeded(resolvedOutcome, commandEndReached);
        }

        emitOutputEvents(parsedOutput);
        this.commandLock.unlock();

        retainResult(parsedOutput, resolvedOutcome);
        if(command.isRequiresCleanup() || commandEndReached) {
            cleanOutput(processOutput);
        }

        return resolvedOutcome;
    }

    /*Extracts variables from the current command and apply them to the current session, for the duration of the command's execution*/
    private void assignContextVariables(Command command){
        this.commandOrdinal++;
        this.commandStartTime = LocalDateTime.now();
        this.currentCommand = command;
        this.evaluatedCommand = CommandUtils.evaluateAgainstDynamicFields(command.getCommandText(), this.getCurrentSessionVariables());
    }

    /*This method assumes we are holding the commandLock for this session

    * Write our current command to the input stream,
    * Each command has a line break character appended to instruct the bash terminal to execute the command
    * This implementation currently writes the command and then flushes it.
    * If writing an excessively long command (more than std_in buffer size) the buffer will fill before it flushes.
    * Keep your commands short*/
    private void writeCommand(ShellChannel channel) throws IOException{
        //Safeguard against writing to channel after termination (can happen in some cases)
        if(!terminatedExternally) {
            try {
                channel.write(this.evaluatedCommand + Literals.LineBreak);
                channel.flush();

                diagnosticManager.emit(new InputSentEvent(this, this.currentCommand, this.commandOrdinal, this.evaluatedCommand));
            } catch (IOException e) {
                sessionLogger.error("Failed to write command " + this.evaluatedCommand + " to channel " + channel.getName() + ". Caused by: " + e.getMessage());
                throw e;
            }
        }
    }

    //This method assumes we are holding the commandLock for this session
    private void sleepMinimalSecondsToResponse(){
        try {
            this.minimalSleepTerminated.await(this.currentCommand.getMinimalSecondsToResponse(), TimeUnit.SECONDS);
        }catch (InterruptedException e){
            //Basically this shouldn't happen, as we use newChunkReceived.signalAll() to interrupt the await() clause
            sessionLogger.warn(Literals.Tab + "Session " + this.getShortSessionId() + " interrupted during the minimal seconds to response for command " + this.commandOrdinal, e.getMessage());
        }
    }

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
    private boolean removeOutdatedChunks(List<String> output){
        if(output.isEmpty()){
            return false;
        }

        String firstLineOfCommand;
        if(this.evaluatedCommand.contains("\n")){
            firstLineOfCommand = this.evaluatedCommand.substring(0, this.evaluatedCommand.indexOf('\n')+1); //if the command contains multiple lines, only search for the first line in the output
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

    /*Parse the command output into a concatenated user-friendly string*/
    private String parsePipedOutput(List<String> pipedProcessOutput){
        if(this.currentCommand.isUseRawOutput()) {
            return String.join(Literals.LineBreak, pipedProcessOutput);
        }else {
            return this.filterRawOutput(pipedProcessOutput);
        }
    }

    /*We could theoretically just apply a ClearingPipe and then a WhitespacePipe
    * But then changes to the pipes could affect the session filtering methods*/
    private String filterRawOutput(List<String> output){
        StringJoiner stringRepresentation = new StringJoiner(" ");
        for(String line : output){
            String filteredLine = line
                    .replace(Literals.CarriageReturn+ Literals.LineBreak, " ")
                    .replace(Literals.LineBreak, " ")
                    .replace(Literals.CarriageReturn, " ")
                    .replace(this.evaluatedCommand, "")
                    .replace(this.currentPrompt, "");

            /*we do not add blank lines to the string representation, to prevent redundant whitespace being inserted into the filtered output
            * case 1 :
            *   output = {"foo", "prompt"};
            *   stringRepresentation (before filter) => "foo prompt"
            *   stringRepresentation (after filter)=> "foo ", but the parsed output should be "foo"
            *
            * case 2 :
             *   output = {"", "", ""};
             *   stringRepresentation => "  " (two space characters), but the parsed output should be ""
            * */
            if(!filteredLine.isBlank()){
                stringRepresentation.add(filteredLine);
            }
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

    /*Check if the command output matches any of our expected logic
     * If a match is found, return the corresponding result for that expected outcome.
     * If no expected outcome achieved (or none exist), return CommandResult.SUCCESS to progress to the next command*/
    private ExpressionResult attemptToResolve(String outputAsString){
        return LogicalExpressionResolver.resolveLogicalExpression(
                outputAsString,
                this.getCurrentSessionVariables(),
                this.currentCommand.getExpectedOutcome()
        );
    }

    /*If successful, if the command returned completely, or if our waiting period had elapsed, return immediately
     * Otherwise, await until a new chunk has been received from the process stream wrapper, or until the command timeout */
    private boolean awaitIfNeeded(ExpressionResult resolvedOutcome, boolean commandEndReached){
        this.elapsedSeconds = this.commandStartTime.until(LocalDateTime.now(), ChronoUnit.SECONDS);
        if(commandEndReached || resolvedOutcome.isResolved() || this.elapsedSeconds >= this.currentCommand.getSecondsToTimeout() - this.currentCommand.getMinimalSecondsToResponse()){
            return true;
        }else {
            try {
                this.newChunkReceived.await(this.currentCommand.getSecondsToTimeout() - this.elapsedSeconds, TimeUnit.SECONDS);
            }catch (InterruptedException e){
                //Basically this shouldn't happen, as we use newChunkReceived.signalAll() to interrupt the await() clause
                sessionLogger.warn(Literals.Tab + "Session " + this.getShortSessionId() + " interrupted while waiting for command " + this.commandOrdinal + " to return. Caused by:", e.getMessage());
            }

            return false;
        }
    }

    private void emitOutputEvents(String parsedOutput){
        if(!terminatedExternally) {
            diagnosticManager.emit(new OutputReceivedEvent(this, this.currentCommand, this.commandOrdinal, parsedOutput));
            diagnosticManager.emit(new OutcomeEvaluationEvent(this, parsedOutput, this.currentCommand.getExpectedOutcome()));
        }
    }

    /*If the command has been resolved, check if the result should be retained in any way, and save it if necessary*/
    private void retainResult(String output, ExpressionResult resolvedOutcome){
        if(terminatedExternally){
            //If terminated externally, the session must stop and the method will return a failure
            resolvedOutcome.withResolved(false)
                .withOutcome(ResultStatus.FAILURE)
                .withMessage(Literals.OperationTerminated);
        }else if(resolvedOutcome.getMessage().equals(Literals.CommandDidNotReachOutcome) && this.elapsedSeconds >= this.currentCommand.getSecondsToTimeout()){
            //If a timeout occurred, the command failed to execute and the method will return a failure
            resolvedOutcome.withResolved(false)
                .withOutcome(ResultStatus.FAILURE)
                .withMessage(Literals.TimeoutInCommand);
        }else if(resolvedOutcome.getOutcome().equals(ResultStatus.SUCCESS)){
            //We clone the retention so that if the command is called again, any action we take within this code block will not affect subsequent executions
            ResultRetention clonedRetention = this.currentCommand.getSaveTo().deepClone();
            if(clonedRetention.getValue().isEmpty()){
                clonedRetention.setValue(CommandUtils.pipeCommandRetention(this, output));
            }else{
                clonedRetention.setValue(CommandUtils.pipeCommandRetention(this, clonedRetention.getValue()));
            }

            //Then parse any dynamic fields declared in the name and value of the cloned retention
            clonedRetention.setName(CommandUtils.evaluateAgainstDynamicFields(clonedRetention.getName(), this.getCurrentSessionVariables()));
            clonedRetention.setValue(CommandUtils.evaluateAgainstDynamicFields(clonedRetention.getValue(), this.getCurrentSessionVariables()));

            //Handle the retention according to the retention type
            if(clonedRetention.getRetentionMode().equals(RetentionMode.Variable)){
                retainToVariable(clonedRetention);
            }else if(clonedRetention.getRetentionMode().equals(RetentionMode.File)){
                retainToFile(clonedRetention);
            }else if(clonedRetention.getRetentionMode().equals(RetentionMode.DatabaseImmediate)){
                retainToDatabaseImmediately(clonedRetention);
            }else if(clonedRetention.getRetentionMode().equals(RetentionMode.DatabaseEventual)){
                retainToDatabaseEventually(clonedRetention);
            }

            //And emit a result retention event
            diagnosticManager.emit(new ResultRetentionEvent(this, clonedRetention));
        }
    }

    private void retainToVariable(ResultRetention clonedRetention){
        String variable = clonedRetention.getName();
        this.sessionVariables.putIfAbsent(variable, new ArrayDeque<>());

        /*Note that variables are scoped to the ICommand in question (unless overwriting)
         * and therefore do NOT generate a database variable */
        Deque<ResultRetention> varStack = this.sessionVariables.get(variable);
        if(!varStack.isEmpty()) {
            varStack.pop();
        }
        varStack.push(clonedRetention);
    }

    private void retainToFile(ResultRetention clonedRetention){
        clonedRetention.setValue(filterFileOutput(clonedRetention.getValue()));
        RetentionFileWriter fileWriter = new RetentionFileWriter(this.getShortSessionId(), clonedRetention.getName(), clonedRetention.getValue());

        try {
            this.threadingManager.submit(fileWriter);
            this.databaseVariables.add(
                new DatabaseVariable()
                    .withDataType(DataType.Path)
                    .withName(clonedRetention.getName())
                    .withValue(Literals.SessionExecutionDir + "/" + this.getShortSessionId() + "/" + clonedRetention.getName())
                    .withCollectedAt(Instant.now())
            );
        } catch (Exception e) {
            sessionLogger.error("Failed to save file " + clonedRetention.getName() + " to file system. Caused by: " + e.getMessage());
        }
    }

    private void retainToDatabaseImmediately(ResultRetention clonedRetention){
        operationProducer.produceRetentionResult(this.operationId, new DatabaseVariable()
            .withDataType(clonedRetention.getDataType())
            .withName(clonedRetention.getName())
            .withValue(clonedRetention.getValue())
            .withCollectedAt(Instant.now())
        );
    }

    private void retainToDatabaseEventually(ResultRetention clonedRetention){
        this.databaseVariables.add(
            new DatabaseVariable()
                .withDataType(clonedRetention.getDataType())
                .withName(clonedRetention.getName())
                .withValue(clonedRetention.getValue())
                .withCollectedAt(Instant.now())
        );
    }

    /*Perform a cleanup on the process output if a cleanup is required (by default or if commandEndReached is true)
     * If commandEndReached is true, the last line is the current prompt; we can safely remove all preceding lines
     * If a cleanup is required, we have no guarantee the last line is not being edited; but we can still safely remove all preceding lines*/
    private void cleanOutput(final List<String> processOutput){
        synchronized (processOutput) {
            int cleanupCounter = processOutput.size() - 1; //all lines before this index (zero-based) will be cleared
            while (cleanupCounter > 0) {
                processOutput.remove(0);
                cleanupCounter--;
            }
        }
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
        for(Map.Entry<String, String> property : properties.entrySet()){
            String name = property.getKey();
            String value = property.getValue();
            this.sessionVariables.putIfAbsent(name, new ArrayDeque<>());
            this.sessionVariables.get(name).push(
                new ResultRetention()
                    .withName(name)
                    .withValue(value)
                    .withRetentionMode(RetentionMode.Variable)
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
        for(Map.Entry<String, Deque<ResultRetention>> sessionVariable : this.sessionVariables.entrySet()){
            String propertyName = sessionVariable.getKey();
            ResultRetention topmostVariable = sessionVariable.getValue().peek();

            if(topmostVariable != null) {
                currentSessionFields.put(propertyName, topmostVariable.getValue());
            }
        }
        return currentSessionFields;
    }

    private String getSessionVariableValue(String sessionVar){
        if(this.sessionVariables.containsKey(sessionVar) && !this.sessionVariables.get(sessionVar).isEmpty()){
            ResultRetention topmostVariable = this.sessionVariables.get(sessionVar).peek();
            if(topmostVariable != null) {
                return topmostVariable.getValue();
            }
        }
        return "";
    }

    public Set<DatabaseVariable> getDatabaseVariables(){
        return Collections.unmodifiableSet(this.databaseVariables);
    }

    @Override
    public boolean isUnderDebug() {
        return isUnderDebug;
    }

    @Override
    public void activateDebugMode() {
        for(ShellChannel channel : getShellChannels().values()){
            channel.activateDebugMode();
        }
        isUnderDebug = true;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isTerminated() {
        return terminatedExternally;
    }

    public void terminate() {
        this.terminatedExternally = true;
    }

    @Override
    public void close() throws IOException{
        boolean partialClosure = false;
        this.commandLock.lock();
        sessionLogger.debug(this.getTerminalIdentifier() + " close method acquired lock");

        for(Map.Entry<String, ShellChannel> channel : this.channels.entrySet()){
            //Try to close each channel in it's own try block, so failure in one channel will not affect other channels
            try {
                channel.getValue().close();
            }catch (IOException e){
                partialClosure = true;
                sessionLogger.error("Session " +  this.getShortSessionId() + " failed to close channel with name " + channel.getKey() +". Caused by: " + e.getMessage());
            }
        }

        try {
            //Immediately interrupt the session if it is currently waiting for anything (minimal seconds / new data from process stream wrapper)
            this.minimalSleepTerminated.signalAll();
            this.newChunkReceived.signalAll();
        }catch(Exception e){
            sessionLogger.error("Session " +  this.getShortSessionId() + " failed to terminate current command. Caused by: " + e.getMessage());
        }

        this.commandLock.unlock();
        sessionLogger.debug(this.getTerminalIdentifier() + " close method released lock");
        this.isClosed = true;
        if(partialClosure){
            throw new IOException("Session " +  this.getShortSessionId() + " failed to close one or more of it's channels");
        }
    }
}
