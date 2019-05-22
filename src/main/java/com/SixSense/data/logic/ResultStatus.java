package com.SixSense.data.logic;

public enum ResultStatus {
    SUCCESS, FAILURE, SKIP;

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
