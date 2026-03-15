package com.ideatrack.main.dto.reviewer;

import lombok.Data;

@Data
public class ReviewerDecisionRequest {
    private Integer reviewerId;
    private String feedback;
    private String decision;
}