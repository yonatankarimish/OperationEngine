package com.sixsense.model.pipes;

import com.sixsense.io.Session;

import java.util.ArrayList;
import java.util.List;

//Replaces all whitespace sequences with a single space in the contents of the output list
public class WhitespacePipe extends AbstractOutputPipe {

    public WhitespacePipe(){
        /*Empty default constructor*/
    }

    @Override
    public List<String> pipe(Session session, List<String> output) {
        List<String> pipedOutput = new ArrayList<>();

        for(String nextLine : output){
            pipedOutput.add(
                nextLine.replaceAll("\\s+", " ")
            );
        }

        return pipedOutput;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WhitespacePipe;
    }
}
