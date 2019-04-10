package com.SixSense.data.pipes;

import java.util.List;

public class LastLinePipe extends AbstractOutputPipe {

    public LastLinePipe(){}

    @Override
    public List<String> pipe(List<String> output) {
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
