package com.sixsense.operation;

import com.sixsense.RemoteConfig;
import com.sixsense.SixSenseBaseTest;
import com.sixsense.model.commands.Operation;
import com.sixsense.model.devices.Credentials;
import com.sixsense.model.retention.OperationResult;
import com.sixsense.io.ProcessStreamWrapper;
import com.sixsense.io.Session;
import com.sixsense.mocks.OperationMocks;
import com.sixsense.utillity.CommandUtils;
import com.sixsense.utillity.MessageLiterals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InputOutputTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(InputOutputTests.class);

    @Test(dataProvider = "f5BigIpConfig", dataProviderClass = RemoteConfig.class, groups = {"operation"})
    public void testChunkSubstitutionCriteria(String host, String username, String password){
        Operation f5Backup = CommandUtils.composeWorkflow(OperationMocks.f5BigIpInventory(
            Collections.singletonList(
                new Credentials()
                    .withHost(host)
                    .withUsername(username)
                    .withPassword(password)
            )
        )).getParallelOperations().get(0); //We can get the first operation, since only one credential set was passed

        Session session = OperationTestUtils.submitOperation(f5Backup);
        ProcessStreamWrapper outputWrapper = session.getShellChannels().get("REMOTE").getChannelOutputWrapper();

        OperationTestUtils.awaitOperation(f5Backup);
        Map<String, String> substitutionCriteria = outputWrapper.getSubstitutionCriteria();
        List<String> rawChunks = outputWrapper.getRawChunks();
        List<String> afterSubstitutions = new ArrayList<>();
        List<String> afterParse = new ArrayList<>();

        for(String chunk : rawChunks){
            for(Map.Entry<String, String> criterion: substitutionCriteria.entrySet()) {
                String pattern = criterion.getKey();
                String replacement = criterion.getValue();
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
