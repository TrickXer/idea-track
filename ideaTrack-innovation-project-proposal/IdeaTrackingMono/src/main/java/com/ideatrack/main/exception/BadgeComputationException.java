package com.ideatrack.main.exception;

/**
 * Thrown when badge calculation cannot complete (e.g., repository failures).
 */
public class BadgeComputationException extends RuntimeException {
    public BadgeComputationException(String message) {
        super(message);
    }
}