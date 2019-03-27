package com.SixSense.util;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

//class used for asynchronous clearing of output and error buffers
public class ResultReader implements Callable<List<String>> {
    private static Logger logger = Logger.getLogger(ResultReader.class);
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