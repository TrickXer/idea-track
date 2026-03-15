package com.ideatrack.main.exception;

/**
 * Thrown when building a gamified profile fails due to data or computation errors.
 */
public class ProfileComputationException extends RuntimeException {
    public ProfileComputationException(String message) {
        super(message);
    }
}