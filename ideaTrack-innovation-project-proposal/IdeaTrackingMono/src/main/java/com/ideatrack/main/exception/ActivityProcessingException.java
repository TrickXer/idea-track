package com.ideatrack.main.exception;

/**
 * Thrown when listing or mapping user activities fails.
 */
public class ActivityProcessingException extends RuntimeException {
    public ActivityProcessingException(String message) {
        super(message);
    }
}