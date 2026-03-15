package com.ideatrack.main.exception;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
