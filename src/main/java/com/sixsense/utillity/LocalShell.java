package com.sixsense.utillity;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/* A simple shell instance, intended to be easier than generating a Session instance
 * can run shell commands supported in linux environments*/
public class LocalShell {
    private static final Logger logger = LogManager.getLogger(LocalShell.class);
    private ProcessBuilder builder = new ProcessBuilder();

    public LocalShell() {
        /*Empty default constructor*/
    }

    //Runs a single command, or a series of chained commands
    //Returns an LocalShellResult with the output and error returned by the operating system
    public synchronized LocalShellResult runCommand(String commandText) throws Exception{
        ExecutorService resultReader = null;
        try {
            Process process = builder.command("/bin/bash", "-c", commandText).start();
            resultReader = Executors.newFixedThreadPool(2);
            Future<List<String>> processOutput = resultReader.submit(new ResultReader(process.getInputStream()));
            Future<List<String>> processErrors = resultReader.submit(new ResultReader(process.getErrorStream()));

            int exitCode = process.waitFor();
            List<String> output = processOutput.get();
            List<String> errors = processErrors.get();
            return new LocalShellResult(exitCode, output, errors);
        }catch (IOException e){
            logger.error("Failed to run command " + commandText + ": Failed to start a shell process. Caused by: " + e.getMessage());
            throw e;
        }catch (InterruptedException e){
            logger.error("Failed to run command " + commandText + ": Interrupted while waiting for command result. Caused by: " + e.getMessage());
            throw e;
        }catch (ExecutionException e){
            logger.error("Failed to run command " + commandText + ": Failed during interaction with IO channels. Caused by: " + e.getMessage());
            throw e;
        }finally {
            if(resultReader != null){
                resultReader.shutdown();
            }
        }
    }

    //Executes an entire script, one command after the other.
    //Commands are independent of each other. i.e. the operation is not atomic or transactional
    //Returns an LocalShellResult with the output and error returned by the operating system for the entire script
    public LocalShellResult runScript(List<String> scriptCommands) throws RuntimeException {
        String referenceToScript = "[shell-script], starting with: "+ scriptCommands.get(0);
        ExecutorService resultReader = null;

        try {
            Process process = builder.command("/bin/bash").start();
            resultReader = Executors.newFixedThreadPool(2);
            Future<List<String>> processOutput = resultReader.submit(new ResultReader(process.getInputStream()));
            Future<List<String>> processErrors = resultReader.submit(new ResultReader(process.getErrorStream()));

            String lastCommand = "[not yet written]";
            try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))){
                //This implementation currently writes the command and then flushes it. If writing an excessively long command (more than std_in buffer size) the buffer will fill before it flushes.
                //Keep your commands short
                for(String command : scriptCommands){
                    logger.debug("LocalShell: Writing command "+command);
                    writer.write(command + MessageLiterals.LineBreak);
                    writer.flush();
                    Thread.sleep(3000);
                }
            }catch (IOException e){
                logger.error("LocalShell: Failed to write command " + lastCommand + ". Caused by: " + e.getMessage());
                throw new RuntimeException(e);
            }

            int exitCode = process.waitFor();
            List<String> output = processOutput.get();
            List<String> errors = processErrors.get();
            return new LocalShellResult(exitCode, output, errors);
        }catch (IOException e){
            logger.error("LocalShell: Failed to run command " + referenceToScript + " - Failed to start a shell process. Caused by: " + e.getMessage());
            throw new RuntimeException(e);
        }catch (InterruptedException e){
            logger.error("LocalShell: Failed to run command " + referenceToScript + " - Interrupted while waiting for command result. Caused by: " + e.getMessage());
            throw new RuntimeException(e);
        }catch (ExecutionException e){
            logger.error("LocalShell: Failed to run command " + referenceToScript + " - Failed during interaction with IO channels. Caused by: " + e.getMessage());
            throw new RuntimeException(e);
        }finally {
            if(resultReader != null){
                resultReader.shutdown();
            }
        }
    }

    //class used for asynchronous clearing of output and error buffers
    private class ResultReader implements Callable<List<String>> {
        private InputStream processStream;

        public ResultReader(InputStream processStream) {
            this.processStream = processStream;
        }

        @Override
        public List<String> call() throws ExecutionException {
            List<String> result = new ArrayList<>();
            String lastResponse = "[not yet read]";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processStream))){
                do {
                    lastResponse = reader.readLine();
                    logger.debug("Just read " + lastResponse);
                    if(lastResponse != null) {
                        result.add(lastResponse);
                    }
                } while (lastResponse != null);

                return result;
            } catch (IOException e){
                logger.error("Failed to process command " + lastResponse + ". Caused by: " + e.getMessage());
                throw new ExecutionException(e);
            }
        }
    }
}