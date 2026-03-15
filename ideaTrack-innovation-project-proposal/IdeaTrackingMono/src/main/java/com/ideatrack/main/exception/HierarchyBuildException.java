package com.ideatrack.main.exception;

/**
 * Thrown when hierarchy (DTO/timeline) building fails due to repository or mapping errors.
 */
public class HierarchyBuildException extends RuntimeException {
    public HierarchyBuildException(String message) {
        super(message);
    }
}