package com.SixSense.data.outcomes;

public enum ResultStatus {
    SUCCESS, FAILURE;

    public ResultStatus invert(){
        if(this.equals(SUCCESS)){
            return FAILURE;
        }else if(this.equals(FAILURE)){
            return SUCCESS;
        }else{
            return this;
        }
    }
}