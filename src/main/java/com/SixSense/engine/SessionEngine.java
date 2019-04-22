package com.SixSense.engine;

import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.ResultStatus;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.io.Session;
import com.SixSense.mocks.LocalhostConfig;
import com.SixSense.queue.WorkerQueue;
import com.SixSense.util.ExpectedOutcomeResolver;
import com.SixSense.util.MessageLiterals;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import java.util.*;
import java.util.concurrent.*;

import static com.SixSense.util.MessageLiterals.SessionPropertiesPath;

@Service
public class SessionEngine implements Closeable, ApplicationContextAware {
    private static final Logger logger = LogManager.getLogger(SessionEngine.class);
    private ApplicationContext appContext;
    @Autowired private WorkflowManager workflowManager;
    @Autowired private WorkerQueue workerQueue;

    private final SSHClient sshClient = new SSHClient();
    private boolean isClosed = false;

    private final Set<String> canceledSessionIds = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Session> runningSessions = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> sessionProperties = new HashMap<>();

    private SessionEngine() throws Exception{
        Path path = Paths.get(SessionPropertiesPath);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            Properties sessionProperties = new Properties();
            sessionProperties.load(reader);

            for(String field: sessionProperties.stringPropertyNames()){
                this.sessionProperties.put(field, sessionProperties.getProperty(field));
            }
        } catch (IOException e) {
            logger.error("Session engine failed to initialize - failed to load session properties. Caused by: ", e);
            throw e;
        }

        try{
            this.sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            this.sshClient.connect(LocalhostConfig.host);
            this.sshClient.authPassword(LocalhostConfig.username, LocalhostConfig.password);
        } catch (IOException e) {
            logger.error("Session engine failed to initialize - failed to initialize ssh client. Caused by: ", e);
            throw e;
        }

        logger.info("Session engine initialized");
    }

    public ExpectedOutcome executeOperation(Operation engineOperation) {
        if(this.isClosed){
            return ExpectedOutcome.executionError("Session engine has been shut down");
        }

        boolean canceledPrematurely = this.canceledSessionIds.remove(engineOperation.getUUID());
        if(canceledPrematurely){
            return ExpectedOutcome.executionError("Operation " + engineOperation.getUUID() + " has ben canceled before reaching session engine");
        }

        if(engineOperation == null || engineOperation.getExecutionBlock() == null){
            return ExpectedOutcome.defaultOutcome();
        }

        try (Session session = (Session)this.appContext.getBean("sixSenseSession")) {
            this.runningSessions.put(engineOperation.getUUID(), session);
            ICommand executionBlock = engineOperation.getExecutionBlock();
            ExpectedOutcome sessionResult;

            if(executionConditionsMet(session, executionBlock)) {
                session.loadSessionDynamicFields(engineOperation);
                sessionResult = this.executeBlock(session, executionBlock);
                sessionResult = expectedResult(sessionResult, engineOperation);
                session.removeSessionDynamicFields(engineOperation);
            }else{
                sessionResult = ExpectedOutcome.skip();
            }

            if(!session.isClosed()) {
                engineOperation.setAlreadyExecuted(true);
            }else{
                sessionResult = ExpectedOutcome.executionError(MessageLiterals.OperationTerminated);
            }

            this.runningSessions.remove(engineOperation.getUUID());
            this.notifyWorkflowManager(engineOperation, sessionResult);
            return sessionResult;

        }catch (Exception e){
            logger.error("SessionEngine - Failed to execute operation " + engineOperation.getFullOperationName() + ". Caused by: ", e);
            return ExpectedOutcome.executionError("SessionEngine - Failed to execute operation " + engineOperation.getFullOperationName() + ". Caused by: " + e.getMessage());
        }
    }

    private ExpectedOutcome executeBlock(Session session, ICommand executionBlock) throws IOException{
        if(session.isClosed()){
            return ExpectedOutcome.executionError(MessageLiterals.OperationTerminated);
        }else if (executionBlock instanceof Command) {
            return this.executeCommand(session, (Command)executionBlock);
        }else if(executionBlock instanceof Block){
            /*the progressive result updates for each of the blocks child commands/blocks
            * progressive + failure = immediate return
            * progressive + skip = progressive
            * progressive + success = success*/
            Block parentBlock = (Block)executionBlock;
            ExpectedOutcome progressiveResult = ExpectedOutcome.defaultOutcome();

            if(executionConditionsMet(session, parentBlock)) {
                session.loadSessionDynamicFields(parentBlock);
                while (!parentBlock.hasExhaustedCommands(session)) {
                    ICommand nextCommand = parentBlock.getNextCommand(session);
                    if (nextCommand != null) {
                        ExpectedOutcome commandResult = this.executeBlock(session, nextCommand);
                        if (commandResult.getOutcome().equals(ResultStatus.FAILURE)) {
                            return commandResult;
                        }else if(!commandResult.equals(ExpectedOutcome.skip())){
                            progressiveResult = commandResult;
                        }
                    }
                }
                session.removeSessionDynamicFields(parentBlock);
            }else{
                progressiveResult = ExpectedOutcome.skip();
            }

            parentBlock.setAlreadyExecuted(true);
            return expectedResult(progressiveResult, executionBlock);
        }else{
            return ExpectedOutcome.executionError(MessageLiterals.InvalidExecutionBlock);
        }
    }

    private ExpectedOutcome executeCommand(Session session, Command currentCommand) throws IOException{
        if(session.isClosed()){
            return ExpectedOutcome.executionError(MessageLiterals.OperationTerminated);
        }

        ExpectedOutcome commandResult;
        if(executionConditionsMet(session, currentCommand)) {
            session.loadSessionDynamicFields(currentCommand);
            commandResult = session.executeCommand(currentCommand);
            session.removeSessionDynamicFields(currentCommand);
        }else{
            commandResult = ExpectedOutcome.skip();
        }

        currentCommand.setAlreadyExecuted(true);
        return commandResult;
    }

    public ExpectedOutcome terminateOperation(String operationID){
        Session terminatingSession = this.runningSessions.remove(operationID);
        if(terminatingSession == null){
            logger.warn("Operation " + operationID + " has no running session, and therefore cannot be terminated");
            this.canceledSessionIds.add(operationID);
        }else{
            try {
                terminatingSession.close();
            } catch (IOException e) {
                logger.error("Operation " + operationID + " failed to terminate session " + terminatingSession.getSessionShellId() + ". Caused by: ", e);
                return ExpectedOutcome.executionError(MessageLiterals.ExceptionEncountered);
            }
        }

        return ExpectedOutcome.executionError(MessageLiterals.OperationTerminated);
    }

    private boolean executionConditionsMet(Session session, ICommand command){
        return ExpectedOutcomeResolver.checkExecutionConditions(
                session.getCurrentSessionVariables(),
                command.getExecutionConditions(),
                command.getConditionAggregation()
        ).isResolved();
    }

    private ExpectedOutcome expectedResult(ExpectedOutcome achievedResult, ICommand parent){
        /*If the result of executing the block was expected by the parent (i.e. they are equal in the weak sense), override it with the parent result
         * Otherwise, return the result as is was returned from the executeBlock() method*/
        if(parent.getExpectedOutcomes() == null){
            return achievedResult;
        }

        for(ExpectedOutcome expectedOutcome : parent.getExpectedOutcomes()){
            if(expectedOutcome.equals(achievedResult)){
                return expectedOutcome;
            }
        }
        return achievedResult;
    }

    @Bean(value="sixSenseSession")
    @Scope("prototype")
    private Session createSession() throws IOException{
        net.schmizz.sshj.connection.channel.direct.Session localSession = sshClient.startSession();
        net.schmizz.sshj.connection.channel.direct.Session remoteSession = sshClient.startSession();

        Session session = new Session(localSession, remoteSession);
        session.setApplicationContext(this.appContext);
        session.loadSessionVariables(this.sessionProperties);

        try {
            Future<Boolean> sessionLocalOutput = this.workerQueue.submit(session.getLocalStreamWrapper());
            Future<Boolean> sessionRemoteOutput = this.workerQueue.submit(session.getRemoteStreamWrapper());
        }catch (Exception e){
            String message = "Failed to create new session - could not submit IO streams to worker queue.";
            logger.error(message, e);
            session.close();
            throw new IOException(message, e);
        }

        return session;
    }

    private void notifyWorkflowManager(Operation operation, ExpectedOutcome resolvedOutcome){
        /*We submit the notify request to the worker queue so that we can return the expected outcome without waiting for the next workflow to complete
        * Since workflows are lengthy operations, this is a major time and resource saver*/
        try {
            workerQueue.submit(() -> workflowManager.notifyWorkflow(operation, resolvedOutcome));
        }catch (Exception e){
            logger.error("Failed to notify workflow manager that operation " + operation.getUUID() + " has completed. Caused by: ", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.appContext = applicationContext;
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
