package com.SixSense.pipes;

import java.util.List;

public class FirstLinePipe extends AbstractOutputPipe {

    public FirstLinePipe(){}

    @Override
    public List<String> pipe(List<String> output) {
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
