package com.sixsense.model.pipes;

import com.sixsense.io.Session;
import com.sixsense.utillity.Literals;

import java.util.List;

public class FirstLinePipe extends AbstractOutputPipe {

    public FirstLinePipe(){
        /*Empty default constructor*/
    }

    @Override
    public String pipe(Session session, String output) {
        if(output == null || output.isEmpty()){
            return output;
        }

        int lineBreakIndex = output.stripLeading().indexOf(Literals.LineBreak);
        if(lineBreakIndex < 0){
            return output;
        }

        return output.stripLeading().substring(0, lineBreakIndex);
    }

    @Override
    public List<String> pipe(Session session, List<String> output) {
        if(output == null || output.isEmpty()){
            return output;
        }

        return output.subList(0, 1);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FirstLinePipe;
    }
}
