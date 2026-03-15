package com.ideatrack.main.exception;

/**
 * Thrown when a requested domain resource (User, Idea, Activity, etc.) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}