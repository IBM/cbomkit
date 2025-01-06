package com.ibm.infrastructure.errors;

public class AggregateReconstructionFailed extends Exception {
    public AggregateReconstructionFailed(Exception e) {
        super("Aggregate reconstruction failed due to ", e);
    }
}
