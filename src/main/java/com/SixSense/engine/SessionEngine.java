package com.SixSense.engine;

import com.SixSense.data.outcomes.ExpectedOutcome;
import com.SixSense.data.outcomes.ResultStatus;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.io.Session;
import com.SixSense.mocks.LocalhostConfig;
import com.SixSense.util.ExpectedOutcomeResolver;
import com.SixSense.util.MessageLiterals;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

import static com.SixSense.util.MessageLiterals.SessionPropertiesPath;

public class SessionEngine implements Closeable{
    private static Logger logger = Logger.getLogger(SessionEngine.class);
    private static SessionEngine engineInstance;
    private final SSHClient sshClient = new SSHClient();
    private final ExecutorService workerPool = Executors.newCachedThreadPool();

    private final Map<String, String> sessionProperties = new HashMap<>();

    private SessionEngine() throws IOException{
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

    public static synchronized SessionEngine getInstance() throws IOException{
        if(engineInstance == null){
            engineInstance = new SessionEngine();
            logger.info("SessionEngine Created");
        }
        return engineInstance;
    }

    public ExpectedOutcome executeOperation(Operation engineOperation) {
        if(this.workerPool.isShutdown()){
            return ExpectedOutcome.executionError("Session engine has been shut down");
        }

        if(engineOperation == null || engineOperation.getExecutionBlock() == null){
            return ExpectedOutcome.defaultOutcome();
        }

        try (Session session = this.createSession()) {
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

            engineOperation.setAlreadyExecuted(true);
            return sessionResult;
        }catch (Exception e){
            logger.error("SessionEngine - Failed to execute operation " + engineOperation.getFullOperationName() + ". Caused by: ", e);
            return ExpectedOutcome.executionError("SessionEngine - Failed to execute operation " + engineOperation.getFullOperationName() + ". Caused by: " + e.getMessage());
        }
    }

    private ExpectedOutcome executeBlock(Session session, ICommand executionBlock) throws IOException{
        if (executionBlock instanceof Command) {
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
                while (!parentBlock.hasExhaustedCommands()) {
                    ICommand nextCommand = parentBlock.getNextCommand();
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

    private Session createSession() throws IOException{
        net.schmizz.sshj.connection.channel.direct.Session localSession = sshClient.startSession();
        net.schmizz.sshj.connection.channel.direct.Session remoteSession = sshClient.startSession();

        Session session = new Session(localSession, remoteSession);
        session.loadSessionVariables(this.sessionProperties);

        Future<Boolean> sessionLocalOutput = workerPool.submit(session.getLocalStreamWrapper());
        Future<Boolean> sessionRemoteOutput = workerPool.submit(session.getRemoteStreamWrapper());

        //executeCommand(session, LocalhostConfig.sessionStartBlock(CommandType.LOCAL));
        //executeCommand(session, LocalhostConfig.sessionStartBlock(CommandType.REMOTE));

        return session;
    }

    @Override
    public void close() {
        this.workerPool.shutdownNow();
        try {
            this.sshClient.close();
        } catch (IOException e) {
            logger.error("Session engine failed to close - failed to close ssh client. Caused by: ", e);
        }

        logger.info("Session engine closed");
    }
}
