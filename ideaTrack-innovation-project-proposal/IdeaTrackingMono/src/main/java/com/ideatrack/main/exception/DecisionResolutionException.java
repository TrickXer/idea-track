package com.ideatrack.main.exception;

/**
 * Thrown when resolving stage-scoped reviewer decisions fails or returns inconsistent data.
 */
public class DecisionResolutionException extends RuntimeException {
    public DecisionResolutionException(String message) {
        super(message);
    }
}