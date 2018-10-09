package util;

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
        try {
            Process process = builder.command("/bin/bash", "-c", commandText).start();
            ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
            Future<List<String>> processOutput = singleExecutor.submit(new ResultReader(process.getInputStream()));
            Future<List<String>> processErrors = singleExecutor.submit(new ResultReader(process.getErrorStream()));

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
        }
    }

    //Executes an entire script, one command after the other.
    //Commands are independent of each other. i.e. the operation is not atomic or transactional
    //Returns an OperationResult with the output and error returned by the operating system for the entire script
    public synchronized OperationResult runScript(List<String> scriptCommands) throws Exception {
        String referenceToScript = "[shell-script], starting with: "+ scriptCommands.get(0);

        try {
            Process process = builder.command("/bin/bash").start();
            ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
            Future<List<String>> processOutput = singleExecutor.submit(new ResultReader(process.getInputStream()));
            Future<List<String>> processErrors = singleExecutor.submit(new ResultReader(process.getErrorStream()));

            String lastCommand = "[not yet written]";
            try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))){
                for(String command : scriptCommands){
                    writer.write(command);
                    writer.flush();
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
        }
    }

    //Internal class used for asynchronous clearing of output and error buffers
    private class ResultReader implements Callable<List<String>> {
        private InputStream processStream;

        public ResultReader(InputStream processStream) {
            this.processStream = processStream;
        }

        @Override
        public List<String> call() throws ExecutionException{
            List<String> result = new ArrayList<>();
            String lastResponse = "[not yet read]";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processStream))){
                do {
                    lastResponse = reader.readLine();
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