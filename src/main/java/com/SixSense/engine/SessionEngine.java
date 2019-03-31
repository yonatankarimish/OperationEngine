package com.SixSense.engine;

import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.data.Outcomes.ResultStatus;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.io.Session;
import com.SixSense.util.MessageLiterals;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.concurrent.*;

public class SessionEngine implements Closeable{
    private static Logger logger = Logger.getLogger(SessionEngine.class);
    private static SessionEngine engineInstance;
    private ProcessBuilder builder = new ProcessBuilder().redirectErrorStream(true); //Creates shell processes, with their error stream merged into their output stream
    private final ExecutorService workerPool = Executors.newCachedThreadPool();

    private SessionEngine(){}

    public static synchronized SessionEngine getInstance(){
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
            ExpectedOutcome sessionResult = this.executeBlock(session, executionBlock);
            return expectedResult(sessionResult, engineOperation);
        }catch (Exception e){
            logger.error("SessionEngine - Failed to execute operation " + engineOperation.getFullOperationName() + ". Caused by: ", e);
            return ExpectedOutcome.executionError("SessionEngine - Failed to execute operation " + engineOperation.getFullOperationName() + ". Caused by: " + e.getMessage());
        }
    }

    private ExpectedOutcome executeBlock(Session session, ICommand executionBlock) throws IOException{
        if (executionBlock instanceof Command) {
            return this.executeCommand(session, (Command)executionBlock);
        }else if(executionBlock instanceof Block){
            Block parentBlock = (Block)executionBlock;
            ExpectedOutcome progressiveResult = ExpectedOutcome.defaultOutcome();

            while(!parentBlock.hasExhaustedCommands()){
                Command nextCommand = parentBlock.getNextCommand();
                if(nextCommand != null){
                    progressiveResult = this.executeCommand(session, nextCommand);
                    if(progressiveResult.getOutcome().equals(ResultStatus.FAILURE)){
                        return  progressiveResult;
                    }
                }
            }

            return expectedResult(progressiveResult, executionBlock);
        }else{
            return ExpectedOutcome.executionError(MessageLiterals.InvalidExecutionBlock);
        }
    }

    private ExpectedOutcome executeCommand(Session session, Command currentCommand) throws IOException{
        return session.executeCommand(currentCommand);
    }

    private ExpectedOutcome expectedResult(ExpectedOutcome achievedResult, ICommand parent){
        /*If the result of executing the block was expected by the parent (i.e. they are equal in the weak sense), override it with the parent result
         * Otherwise, return the result as is was returned from the executeBlock() method*/
        if(parent.getExpectedOutcomes() == null){
            return achievedResult;
        }

        for(ExpectedOutcome expectedOutcome : parent.getExpectedOutcomes()){
            if(expectedOutcome.weakEquals(achievedResult)){
                return expectedOutcome;
            }
        }
        return achievedResult;
    }

    private Session createSession() throws IOException{
        Process localProcess = builder.command("/bin/bash").start();
        Process remoteProcess = builder.command("/bin/bash").start();
        Session session = new Session(localProcess, remoteProcess);
        Future<Boolean> sessionLocalOutput = workerPool.submit(session.getLocalOutputAndErrors());
        Future<Boolean> sessionRemoteOutput = workerPool.submit(session.getRemoteOutputAndErrors());
        return session;
    }

    @Override
    public void close() {
        this.workerPool.shutdownNow();
    }
}
