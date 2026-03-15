package com.ideatrack.main.exception;

public class SubmitTimelineMissingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SubmitTimelineMissingException() {
        super("Missing/invalid fields: timeLineStart, timeLineEnd");
    }
}