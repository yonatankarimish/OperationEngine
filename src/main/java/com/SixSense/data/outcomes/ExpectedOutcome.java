package com.SixSense.data.outcomes;

public class ExpectedOutcome implements IFlowConnector {
    private boolean resolved; //Did the expected outcome turn out to be true?
    private ResultStatus outcome; //If the expected outcome has been resolved, what should be the outcome of the command?
    private String message; //Arbitrary message, if the expected outcome has been resolved
    private String expectedValue;
    private BinaryRelation binaryRelation;

    //The default expected outcome wills search for the empty string in any result, therefore always returning CommandResult.SUCCESS.
    public ExpectedOutcome() {
        this.resolved = false;
        this.outcome = ResultStatus.SUCCESS;
        this.message = "";
        this.expectedValue = "";
        this.binaryRelation = BinaryRelation.CONTAINS;
    }

    public ExpectedOutcome(String expectedValue, BinaryRelation binaryRelation, ResultStatus outcome, String message) {
        this.resolved = false;
        this.outcome = outcome;
        this.message = message;
        this.expectedValue = expectedValue;
        this.binaryRelation = binaryRelation;
    }

    public static ExpectedOutcome defaultOutcome(){
        return new ExpectedOutcome()
                .withResolved(true)
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("")
                .withExpectedValue("")
                .withBinaryRelation(BinaryRelation.CONTAINS);
    }

    public static ExpectedOutcome skip(){
        return new ExpectedOutcome()
                .withResolved(true)
                .withOutcome(ResultStatus.SUCCESS)
                .withMessage("")
                .withExpectedValue("")
                .withBinaryRelation(BinaryRelation.NONE);
    }

    public static ExpectedOutcome executionError(String errorMessage){
        return new ExpectedOutcome()
                .withResolved(false)
                .withOutcome(ResultStatus.FAILURE)
                .withMessage(errorMessage)
                .withExpectedValue("")
                .withBinaryRelation(BinaryRelation.NONE);
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    @Override
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
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

    @Override
    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public ExpectedOutcome withExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
        return this;
    }

    @Override
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
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        else if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return equals((ExpectedOutcome) other);
    }

    public boolean equals(ExpectedOutcome otherOutcome) {
        return weakEquals(otherOutcome)
                && this.expectedValue.equals(otherOutcome.expectedValue)
                && this.binaryRelation.equals(otherOutcome.binaryRelation);
    }

    public boolean weakEquals(ExpectedOutcome otherOutcome) {
        return this.resolved == otherOutcome.resolved
                && this.outcome.equals(otherOutcome.outcome);
    }

    public boolean strongEquals(ExpectedOutcome otherOutcome) {
        return equals(otherOutcome)
                && this.message.equals(otherOutcome.message);
    }

    @Override
    public String toString() {
        return "ExpectedOutcome{" +
                "resolved=" + resolved +
                ", outcome=" + outcome +
                ", message='" + message + '\'' +
                ", expectedValue='" + expectedValue + '\'' +
                ", binaryRelation=" + binaryRelation +
                '}';
    }
}
