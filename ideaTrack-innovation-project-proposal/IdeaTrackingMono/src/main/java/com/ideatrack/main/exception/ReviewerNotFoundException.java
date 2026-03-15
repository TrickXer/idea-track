package com.ideatrack.main.exception;

import org.springframework.http.HttpStatus;

public class ReviewerNotFoundException extends ReviewerException {
    public ReviewerNotFoundException(String code, String msg) {
        super(code, HttpStatus.NOT_FOUND, msg);
    }
}