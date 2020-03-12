package com.SixSense.engine;

import com.SixSense.data.events.*;
import com.SixSense.data.logic.*;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.retention.OperationResult;
import com.SixSense.io.ProcessStreamWrapper;
import com.SixSense.io.Session;
import com.SixSense.io.ShellChannel;
import com.SixSense.queue.WorkerQueue;
import com.SixSense.util.LogicalExpressionResolver;
import com.SixSense.util.MessageLiterals;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.SixSense.util.MessageLiterals.SessionPropertiesPath;

@Service
/*Creates sessions and executes operations*/
public class SessionEngine implements Closeable, ApplicationContextAware {
    private static final Logger logger = LogManager.getLogger(SessionEngine.class);
    private ApplicationContext appContext;
    private final WorkerQueue workerQueue;
    private final DiagnosticManager diagnosticManager;

    private final SSHClient sshClient = new SSHClient();
    private boolean isClosed = false;

    private static final Map<String, String> sessionProperties = new ConcurrentHashMap<>();
    private final Map<String, Operation> runningOperations = new ConcurrentHashMap<>(); //key: operation id, value: operation
    private final Map<String, Session> runningSessions = new ConcurrentHashMap<>(); //key: session id, value: session
    private final Map<String, String> operationsToSessions = new ConcurrentHashMap<>(); //key: operation id, value: session id

    @Autowired
    private SessionEngine(WorkerQueue workerQueue, DiagnosticManager diagnosticManager) throws Exception{
        this.workerQueue = workerQueue;
        this.diagnosticManager = diagnosticManager;
        Path sessionProps = Paths.get(SessionPropertiesPath);
        try (BufferedReader reader = Files.newBufferedReader(sessionProps)) {
            Properties sessionProperties = new Properties();
            sessionProperties.load(reader);

            for(String field: sessionProperties.stringPropertyNames()){
                this.sessionProperties.put(field, sessionProperties.getProperty(field));
            }
        } catch (IOException e) {
            logger.error("Session engine failed to initialize - failed to load session properties. Caused by: ", e);
            throw e;
        }

        try (InputStream stream = ClassLoader.getSystemClassLoader().getResource("localhost.properties").openStream()){
            Properties localhostProperties = new Properties();
            localhostProperties.load(stream);

            logger.info("enumerating localhost.properties");
            for(String field: localhostProperties.stringPropertyNames()){
                logger.info("enumerated localhost property " + field + ": " + localhostProperties.getProperty(field));
            }
            this.sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            this.sshClient.connect(localhostProperties.getProperty("local.host"), Integer.valueOf(localhostProperties.getProperty("local.port")));
            this.sshClient.authPassword(localhostProperties.getProperty("local.username"), localhostProperties.getProperty("local.password"));
        } catch (IOException e) {
            logger.error("Session engine failed to initialize - failed to initialize ssh client. Caused by: ", e);
            throw e;
        } catch (NullPointerException e) {
            logger.error("Session engine failed to initialize - failed reading localhost properties. Caused by: ", e);
            throw e;
        }

        logger.info("Session engine initialized");
    }

    public OperationResult executeOperation(Operation operation){
        Session session = null;
        OperationResult operationResult;

        try {
            session = initializeSession(operation); //Initiate a new session (will finalize if failed to initialize)
            operationResult = executeOperation(session, operation); //use the new session to execute the operation
            finalizeSession(session); //finalize the session and release any resources generated by the session
        } catch (Exception e){
            String errorMessage = "SessionEngine - Failed to execute operation " + operation.getOperationName() + ". Caused by: ";
            logger.error(errorMessage, e);
            operationResult = new OperationResult().withExpressionResult(
                this.handleExecutionAnomaly(session, errorMessage + e.getMessage())
            );
        }

        return operationResult;
    }

    public OperationResult executeOperation(Session session, Operation operation) {
        OperationResult operationResult = new OperationResult();

        if(operation == null){
            operationResult.setExpressionResult(
                handleExecutionAnomaly(session, "Session engine cannot execute a null operation!")
            );
        }else if(this.isClosed){
            operationResult.setExpressionResult(
                handleExecutionAnomaly(session, MessageLiterals.EngineShutdown)
            );
        }else if(session.isClosed()){
            operationResult.setExpressionResult(
                handleExecutionAnomaly(session, MessageLiterals.OperationTerminated)
            );
        }else if(operation.getExecutionBlock() == null){
            operationResult.setExpressionResult(
                handleExecutionAnomaly(session, "Operation " + operation.getUUID() + " has incomplete configuration")
            );
        }else{
            this.runningSessions.put(session.getSessionShellId(), session);
            this.runningOperations.put(operation.getUUID(), operation);
            this.operationsToSessions.put(operation.getUUID(), session.getSessionShellId());

            session.incrementDrilldownRank();
            diagnosticManager.emit(new OperationStartEvent(session, operation));

            try {
                ICommand executionBlock = operation.getExecutionBlock();
                session.incrementDrilldownRank();
                session.loadSessionDynamicFields(operation);

                diagnosticManager.emit(new ConditionEvaluationEvent(session, operation.getExecutionCondition()));
                if (executionConditionsMet(session, operation)) {
                    operationResult.setExpressionResult(
                        executeBlock(session, executionBlock)
                    );
                    operationResult.setExpressionResult(
                        expectedResult(operationResult.getExpressionResult(), operation.getExpectedOutcome().getExpressionResult())
                    );
                } else {
                    operationResult.setExpressionResult(
                        ExpressionResult.skip()
                    );
                }

                diagnosticManager.emit(new OutcomeEvaluationEvent(session, "", operation.getExpectedOutcome()));
                session.removeSessionDynamicFields(operation);
                session.decrementDrilldownRank();
                operation.setAlreadyExecuted(true);
            } catch (Exception e) {
                String errorMessage = "SessionEngine - Failed to execute operation " + operation.getOperationName() + ". Caused by: ";
                logger.error(errorMessage, e);
                operationResult.setExpressionResult(
                    handleExecutionAnomaly(session, errorMessage + e.getMessage())
                );
            }

            operationResult.addDatabaseVariables(session.getDatabaseVariables());
            diagnosticManager.emit(new OperationEndEvent(session, operation, operationResult));
            session.decrementDrilldownRank();

            this.operationsToSessions.remove(operation.getUUID());
            this.runningOperations.remove(operation.getUUID());
            this.runningSessions.remove(session.getSessionShellId());
        }

        return operationResult;
    }

    private ExpressionResult executeBlock(Session session, ICommand executionBlock) throws IOException{
        ExpressionResult blockResult = null;
        if(this.isClosed){
            blockResult = handleExecutionAnomaly(session, MessageLiterals.EngineShutdown);
        }else if(session.isClosed()){
            blockResult = handleExecutionAnomaly(session, MessageLiterals.OperationTerminated);
        }else if (executionBlock instanceof Command) {
            blockResult = executeCommand(session, (Command)executionBlock);
        }else if(executionBlock instanceof Block){
            /*the progressive result updates for each of the blocks child commands/blocks
            * progressive + failure = immediate return
            * progressive + skip = progressive
            * progressive + success = success*/
            Block parentBlock = (Block)executionBlock;
            ExpressionResult progressiveResult = ExpressionResult.defaultOutcome();
            diagnosticManager.emit(new BlockStartEvent(session, parentBlock));

            session.incrementDrilldownRank();
            session.loadSessionDynamicFields(parentBlock);
            diagnosticManager.emit(new ConditionEvaluationEvent(session, parentBlock.getExecutionCondition()));
            if(executionConditionsMet(session, parentBlock)) {
                while (!parentBlock.hasExhaustedCommands(session)) {
                    ICommand nextCommand = parentBlock.getNextCommand(session);
                    if (nextCommand != null) {
                        ExpressionResult commandResult = executeBlock(session, nextCommand);
                        if (commandResult.getOutcome().equals(ResultStatus.FAILURE)){
                            blockResult = commandResult;
                            break;
                        }else if(!commandResult.getOutcome().equals(ResultStatus.SKIP)){
                            progressiveResult = commandResult;
                        }
                    }
                }
            }else{
                progressiveResult = ExpressionResult.skip();
            }

            session.removeSessionDynamicFields(parentBlock);
            session.decrementDrilldownRank();
            parentBlock.setAlreadyExecuted(true);
            if(blockResult == null) {// which (before this line) can only be assigned if commandResult has ResultStatus.FAILURE
                blockResult = expectedResult(progressiveResult, parentBlock.getExpectedOutcome().getExpressionResult());
            }

            diagnosticManager.emit(new OutcomeEvaluationEvent(session, "", parentBlock.getExpectedOutcome()));
            diagnosticManager.emit(new BlockEndEvent(session, parentBlock, blockResult));
        }else{
            blockResult = handleExecutionAnomaly(session, MessageLiterals.InvalidExecutionBlock);
        }
        return blockResult;
    }

    private ExpressionResult executeCommand(Session session, Command currentCommand) throws IOException{
        ExpressionResult commandResult;

        if(this.isClosed){
            commandResult = handleExecutionAnomaly(session, MessageLiterals.EngineShutdown);
        }else if(session.isClosed()){
            commandResult = handleExecutionAnomaly(session, MessageLiterals.OperationTerminated);
        }else{
            diagnosticManager.emit(new CommandStartEvent(session, currentCommand));

            session.incrementDrilldownRank();
            session.loadSessionDynamicFields(currentCommand);
            diagnosticManager.emit(new ConditionEvaluationEvent(session, currentCommand.getExecutionCondition()));
            if (executionConditionsMet(session, currentCommand)) {
                commandResult = session.executeCommand(currentCommand);
            } else {
                commandResult = ExpressionResult.skip();
            }

            session.removeSessionDynamicFields(currentCommand);
            session.decrementDrilldownRank();
            currentCommand.setAlreadyExecuted(true);
            diagnosticManager.emit(new CommandEndEvent(session, currentCommand, commandResult));
        }

        return commandResult;
    }

    private boolean executionConditionsMet(Session session, ICommand command){
        return LogicalExpressionResolver.resolveLogicalExpression(
                session.getCurrentSessionVariables(),
                command.getExecutionCondition()
        ).isResolved();
    }

    private ExpressionResult expectedResult(ExpressionResult achievedResult, ExpressionResult parentResult){
        /*If the result of executing the block was expected by the parent (i.e. they are equal in the weak sense), override it with the parent result
         * Otherwise, return the result as is was returned from the executeBlock() method*/
        if(parentResult.strongEquals(ExpressionResult.defaultOutcome())){
            return achievedResult;
        } else if(achievedResult.equals(parentResult)){
            return parentResult;
        }else{
            return achievedResult;
        }
    }

    private ExpressionResult handleExecutionAnomaly(Session session, String message){
        ExpressionResult executionError = ExpressionResult.executionError(message);
        diagnosticManager.emit(new ExecutionAnomalyEvent(session, executionError));
        return executionError;
    }

    //Attempts to create a new session
    public Session initializeSession(Operation operation) throws Exception{
        Session session;
        try{
            session = (Session) this.appContext.getBean("sixSenseSession", operation);
            diagnosticManager.emit(new SessionCreatedEvent(session));
        } catch (BeansException e){
            logger.error("SessionEngine - Failed to initialize a new session for operation " + operation.getOperationName() + ". Caused by: ", e);
            throw new Exception(e);
        }

        return session;
    }

    //Create a prototype session bean, generate the required I/O channels and load session variables. If fails, will finalize the session to prevent it from executing commands
    @Bean(value="sixSenseSession")
    @Scope("prototype")
    private Session createSession(Operation operation) throws IOException, NullPointerException{
        if(operation == null){
            throw new NullPointerException("Cannot create a session using a null operation!");
        }

        Session session = new Session(this.sshClient, operation.getChannelNames());
        session.loadSessionVariables(sessionProperties);
        ThreadContext.put("sessionID", session.getSessionShellId());

        try {
            List<ProcessStreamWrapper> wrappers = session.getShellChannels().values().stream()
                    .map(ShellChannel::getChannelOutputWrapper)
                    .collect(Collectors.toList());

            for(ProcessStreamWrapper wrapper : wrappers){
                this.workerQueue.submit(wrapper);
            }
        }catch (Exception e){
            String message = "Failed to create new session - could not submit channel IO streams to worker queue.";
            logger.error(message, e);

            finalizeSession(session);
            throw new IOException(message, e);
        }

        return session;
    }

    public void finalizeSession(Session session) throws IOException{
        try {
            session.close();
            diagnosticManager.emit(new SessionClosedEvent(session));
        } catch (IOException e) {
            logger.error("SessionEngine - Failed to finalize session with id " + session.getSessionShellId() + ". Caused by: ", e);
            throw e;
        } finally {
            ThreadContext.remove("sessionID");
        }
    }

    public OperationResult terminateOperation(String operationID){
        ExpressionResult terminationResult = ExpressionResult.executionError(MessageLiterals.OperationTerminated);
        String sessionID = this.operationsToSessions.get(operationID);

        if(sessionID == null) {
            logger.warn("Operation " + operationID + " has no running session, and therefore cannot be terminated");
        }else{
            this.runningOperations.remove(operationID);
            Session terminatingSession = this.runningSessions.remove(sessionID);
            if (terminatingSession == null) {
                logger.warn("Session " + sessionID + " is not currently running, and therefore cannot be terminated");
            } else {
                try {
                    terminatingSession.terminate();
                    finalizeSession(terminatingSession);
                } catch (IOException e) {
                    logger.error("Failed to terminate session " + terminatingSession.getSessionShellId() + ". Caused by: ", e);
                    terminationResult = handleExecutionAnomaly(terminatingSession, MessageLiterals.ExceptionEncountered);
                }
            }
        }

        return new OperationResult().withExpressionResult(terminationResult);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.appContext = applicationContext;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public static Map<String, String> getSessionProperties(){
        return Collections.unmodifiableMap(sessionProperties);
    }

    public static Map<String, String> addSessionProperties(Map<String, String> updatedConfig) throws IOException{
        Path sessionProps = Paths.get(SessionPropertiesPath);
        try (BufferedReader reader = Files.newBufferedReader(sessionProps);
            BufferedWriter writer = Files.newBufferedWriter(sessionProps, StandardOpenOption.WRITE)) {
            Properties sessionProperties = new Properties();
            sessionProperties.load(reader);

            for(String field: updatedConfig.keySet()){
                sessionProperties.put(field, updatedConfig.get(field));
            }

            sessionProperties.store(writer, "");
        } catch (IOException e) {
            logger.error("Session engine failed to initialize - failed to load session properties. Caused by: ", e);
            throw e;
        }

        sessionProperties.putAll(updatedConfig);
        return Collections.unmodifiableMap(sessionProperties);
    }

    public Map<String, Operation> getRunningOperations() {
        return Collections.unmodifiableMap(runningOperations);
    }

    public Map<String, String> getOperationsToSessions(){
        return Collections.unmodifiableMap(operationsToSessions);
    }

    @Override
    public void close() {
        try {
            this.sshClient.close();
        } catch (IOException e) {
            logger.error("Session engine failed to close - failed to close ssh client. Caused by: ", e);
        }

        this.isClosed = true;
        logger.info("Session engine closed");
    }
}
