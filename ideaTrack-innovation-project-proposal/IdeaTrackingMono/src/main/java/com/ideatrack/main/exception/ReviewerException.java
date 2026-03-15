package com.ideatrack.main.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ReviewerException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public ReviewerException(String errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
