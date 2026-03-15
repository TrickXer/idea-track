package com.ideatrack.main.exception;

/**
 * Thrown for profile photo upload/delete file-system errors and invalid file types.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }
}