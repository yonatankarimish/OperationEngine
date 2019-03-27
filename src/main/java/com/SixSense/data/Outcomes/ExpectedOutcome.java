package com.SixSense.data.Outcomes;

public class ExpectedOutcome {
    private boolean resolved; //Did the expected outcome turn out to be true?
    private ResultStatus outcome; //If the expected outcome has been resolved, what should be the outcome of the command?
    private String message; //Arbitrary message, if the expected outcome has been resolved
    private String expectedOutput;
    private BinaryRelation binaryRelation;

    //The default expected outcome wills search for the empty string in any result, therefore always returning CommandResult.SUCCESS.
    public ExpectedOutcome() {
        this.resolved = false;
        this.outcome = ResultStatus.SUCCESS;
        this.message = "";
        this.expectedOutput = "";
        this.binaryRelation = BinaryRelation.CONTAINS;
    }

    public ExpectedOutcome(String expectedOutput, BinaryRelation binaryRelation, ResultStatus outcome, String message) {
        this.resolved = false;
        this.outcome = outcome;
        this.message = message;
        this.expectedOutput = expectedOutput;
        this.binaryRelation = binaryRelation;
    }

    public static ExpectedOutcome defaultOutcome(){
        return new ExpectedOutcome()
                .withResolved(true)
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("")
                .withExpectedOutput("")
                .withBinaryRelation(BinaryRelation.CONTAINS);
    }

    public static ExpectedOutcome executionError(String errorMessage){
        return new ExpectedOutcome()
                .withResolved(false)
                .withOutcome(ResultStatus.FAILURE)
                .withMessage(errorMessage)
                .withExpectedOutput("")
                .withBinaryRelation(BinaryRelation.NONE);
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public ExpectedOutcome withResolved(boolean resolved) {
        this.resolved = resolved;
        return this;
    }

    public ResultStatus getOutcome() {
        return outcome;
    }

    public void setOutcome(ResultStatus outcome) {
        this.outcome = outcome;
    }

    public ExpectedOutcome withOutcome(ResultStatus resultStatus) {
        this.outcome = resultStatus;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ExpectedOutcome withMessage(String message) {
        this.message = message;
        return this;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public ExpectedOutcome withExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
        return this;
    }

    public BinaryRelation getBinaryRelation() {
        return binaryRelation;
    }

    public void setBinaryRelation(BinaryRelation binaryRelation) {
        this.binaryRelation = binaryRelation;
    }

    public ExpectedOutcome withBinaryRelation(BinaryRelation binaryRelation) {
        this.binaryRelation = binaryRelation;
        return this;
    }

    @Override
    public String toString() {
        return "ExpectedOutcome{" +
                "resolved=" + resolved +
                ", outcome=" + outcome +
                ", message='" + message + '\'' +
                ", expectedOutput='" + expectedOutput + '\'' +
                ", binaryRelation=" + binaryRelation +
                '}';
    }
}
