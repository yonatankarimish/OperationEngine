package com.SixSense.engine;

import com.SixSense.RemoteConfig;
import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.devices.Credentials;
import com.SixSense.data.events.AbstractEngineEvent;
import com.SixSense.data.logic.ResultStatus;
import com.SixSense.data.retention.OperationResult;
import com.SixSense.io.ProcessStreamWrapper;
import com.SixSense.io.Session;
import com.SixSense.mocks.TestingMocks;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.MessageLiterals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InputOutputTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(InputOutputTests.class);

    @Test(dataProvider = "f5BigIpConfig", dataProviderClass = RemoteConfig.class, groups = {"engine"})
    public void testChunkSubstitutionCriteria(String host, String username, String password){
        Operation f5Backup = CommandUtils.composeWorkflow(TestingMocks.f5BigIpInventory(
            Collections.singletonList(
                new Credentials()
                    .withHost(host)
                    .withUsername(username)
                    .withPassword(password)
            )
        )).getParallelOperations().get(0); //We can get the first operation, since only one credential set was passed

        Session session = EngineTestUtils.submitOperation(f5Backup);
        ProcessStreamWrapper outputWrapper = session.getShellChannels().get("REMOTE").getChannelOutputWrapper();

        OperationResult operationResult = EngineTestUtils.awaitOperation(session);
        Map<String, String> substitutionCriteria = outputWrapper.getSubstitutionCriteria();
        List<String> rawChunks = outputWrapper.getRawChunks();
        List<String> afterSubstitutions = new ArrayList<>();
        List<String> afterParse = new ArrayList<>();

        for(String chunk : rawChunks){
            for(String pattern : substitutionCriteria.keySet()) {
                String replacement = substitutionCriteria.get(pattern);
                chunk = chunk.replaceAll(pattern, replacement);
            }
            afterSubstitutions.add(chunk);
        }

        for(String chunk : afterSubstitutions){
            String[] splitChunk = (" " + chunk + " ").split(MessageLiterals.LineBreak);
            if(splitChunk.length > 0) {
                String firstChunk = splitChunk[0].trim();
                if (afterParse.isEmpty()) {
                    afterParse.add(firstChunk);
                } else {
                    String lastLine = afterParse.get(afterParse.size() - 1);
                    afterParse.set(afterParse.size() - 1, lastLine + firstChunk);
                }

                int chunkIdx;
                for (chunkIdx = 1; chunkIdx < splitChunk.length; chunkIdx++) {
                    afterParse.add(splitChunk[chunkIdx].trim());
                }
            }
        }
    }
}
