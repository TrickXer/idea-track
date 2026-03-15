package com.ideatrack.main.exception;

import org.springframework.http.HttpStatus;

public class ReviewerBadRequestException extends ReviewerException {
    public ReviewerBadRequestException(String code, String msg) {
        super(code, HttpStatus.BAD_REQUEST, msg);
    }
}