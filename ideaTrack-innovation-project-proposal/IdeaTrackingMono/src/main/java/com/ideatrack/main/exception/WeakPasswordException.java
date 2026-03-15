package com.ideatrack.main.exception;

public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) {
        super(message);
    }
}