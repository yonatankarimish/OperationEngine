package com.sixsense.model.pipes;

import com.sixsense.io.Session;
import com.sixsense.utillity.Literals;

import java.util.List;

public class LastLinePipe extends AbstractOutputPipe {

    public LastLinePipe(){
        /*Empty default constructor*/
    }

    @Override
    public String pipe(Session session, String output) {
        if(output == null || output.isEmpty()){
            return output;
        }

        int lineBreakIndex = output.stripTrailing().lastIndexOf(Literals.LineBreak);
        if(lineBreakIndex < 0){
            return output;
        }

        return output.stripTrailing().substring(lineBreakIndex + 1);
    }

    @Override
    public List<String> pipe(Session session, List<String> output) {
        if(output == null || output.isEmpty()){
            return output;
        }

        for(int line = output.size()-1; line>=0; line--){
            if(!output.get(line).isEmpty()){
                return output.subList(line, line+1);
            }
        }

        return output;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FirstLinePipe;
    }
}
