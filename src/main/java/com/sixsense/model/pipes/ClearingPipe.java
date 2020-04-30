package com.sixsense.model.pipes;

import com.sixsense.io.Session;

import java.util.ArrayList;
import java.util.List;

//Clears the current prompt and the current command from the contents of the output list
public class ClearingPipe extends AbstractOutputPipe {
    @Override
    public List<String> pipe(Session session, List<String> output) {
        List<String> pipedOutput = new ArrayList<>();

        for(String nextLine : output){
            pipedOutput.add(
                nextLine
                    .replace(session.getCurrentEvaluatedCommand(), "")
                    .replace(session.getCurrentPrompt(), "")
            );
        }

        return pipedOutput;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClearingPipe;
    }
}
