package com.ideatrack.main.exception;

public class DuplicateEmailException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateEmailException(String message) {
        super(message);
    }

    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}