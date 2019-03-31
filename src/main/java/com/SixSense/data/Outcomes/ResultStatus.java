package com.SixSense.data.Outcomes;

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
