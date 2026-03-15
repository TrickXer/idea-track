package com.ideatrack.main.exception;

/**
 * Thrown when password change preconditions fail or the persistence update cannot be performed.
 */
public class PasswordChangeException extends RuntimeException {
    public PasswordChangeException(String message) {
        super(message);
    }
}