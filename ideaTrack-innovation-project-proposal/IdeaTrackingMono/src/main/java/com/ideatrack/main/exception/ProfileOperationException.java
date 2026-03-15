package com.ideatrack.main.exception;

/**
 * Thrown for profile CRUD operations (load/update/delete) when validation or persistence fails.
 */
public class ProfileOperationException extends RuntimeException {
    public ProfileOperationException(String message) {
        super(message);
    }
}