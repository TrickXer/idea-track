package com.ideatrack.main.exception;

import org.springframework.http.HttpStatus;

public class ReviewerForbiddenException extends ReviewerException {
    public ReviewerForbiddenException(String code, String msg) {
        super(code, HttpStatus.FORBIDDEN, msg);
    }
}