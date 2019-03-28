package com.SixSense.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


//Used for running shell commands in linux enviromnents
public class OperatingSystem {
    private static Logger logger = Logger.getLogger(OperatingSystem.class);
    private ProcessBuilder builder = new ProcessBuilder();

    public OperatingSystem() {
    }

    //Runs a single command, or a series of chained commands
    //Returns an OperationResult with the output and error returned by the operating system
    public synchronized OperationResult runCommand(String commandText) throws Exception{
        ExecutorService resultReader = null;
        try {
            Process process = builder.command("/bin/bash", "-c", commandText).start();
            resultReader = Executors.newFixedThreadPool(2);
            Future<List<String>> processOutput = resultReader.submit(new ResultReader(process.getInputStream()));
            Future<List<String>> processErrors = resultReader.submit(new ResultReader(process.getErrorStream()));

            int exitCode = process.waitFor();
            List<String> output = processOutput.get();
            List<String> errors = processErrors.get();
            return new OperationResult(exitCode, output, errors);
        }catch (IOException e){
            logger.error("Failed to run command " + commandText + ": Failed to start a shell process. Caused by: ", e);
            throw e;
        }catch (InterruptedException e){
            logger.error("Failed to run command " + commandText + ": Interrupted while waiting for command result. Caused by: ", e);
            throw e;
        }catch (ExecutionException e){
            logger.error("Failed to run command " + commandText + ": Failed during interaction with IO channels. Caused by: ", e);
            throw e;
        }finally {
            if(resultReader != null){
                resultReader.shutdown();
            }
        }
    }

    //Executes an entire script, one command after the other.
    //Commands are independent of each other. i.e. the operation is not atomic or transactional
    //Returns an OperationResult with the output and error returned by the operating system for the entire script
    public OperationResult runScript(List<String> scriptCommands) throws Exception {
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
                    logger.info("Writing command "+command);
                    writer.write(command);
                    writer.flush();
                    Thread.sleep(3000);
                }
            }catch (IOException e){
                logger.error("Failed to write command " + lastCommand + ". Caused by: ", e);
                throw new Exception(e);
            }

            int exitCode = process.waitFor();
            List<String> output = processOutput.get();
            List<String> errors = processErrors.get();
            return new OperationResult(exitCode, output, errors);
        }catch (IOException e){
            logger.error("Failed to run command " + referenceToScript + ": Failed to start a shell process. Caused by: ", e);
            throw e;
        }catch (InterruptedException e){
            logger.error("Failed to run command " + referenceToScript + ": Interrupted while waiting for command result. Caused by: ", e);
            throw e;
        }catch (ExecutionException e){
            logger.error("Failed to run command " + referenceToScript + ": Failed during interaction with IO channels. Caused by: ", e);
            throw e;
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
                    logger.info("Just read " + lastResponse);
                    if(lastResponse != null) {
                        result.add(lastResponse);
                    }
                } while (lastResponse != null);

                return result;
            } catch (IOException e){
                logger.error("Failed to process command " + lastResponse + ". Caused by: ", e);
                throw new ExecutionException(e);
            }
        }
    }
}