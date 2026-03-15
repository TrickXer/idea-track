package com.ideatrack.main.exception;

/**
 * Thrown when XP apply/undo operations fail due to data layer errors or invalid state.
 */
public class XPUpdateException extends RuntimeException {
    public XPUpdateException(String message) {
        super(message);
    }
}