package com.ideatrack.main.dto.reviewer;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewerErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String errorCode;
    private String message;
    private String path;
}