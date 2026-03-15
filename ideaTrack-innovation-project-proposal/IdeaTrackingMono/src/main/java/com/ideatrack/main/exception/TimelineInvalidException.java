package com.ideatrack.main.exception;

import java.time.LocalDate;

public class TimelineInvalidException extends RuntimeException {
    public TimelineInvalidException(LocalDate start, LocalDate end) {
        super("timeLineStart must be on/before timeLineEnd. start=" + start + ", end=" + end);
    }
}