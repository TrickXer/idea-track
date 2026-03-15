package com.ideatrack.main.dto.reviewer;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ReviewerDashboardDTO {
    private Integer ideaId;
    private String ideaTitle;
    private String employeeName;
    private String categoryName;
    private Integer assignmentStage;
    private String currentIdeaStatus;
    private String reviewerDecision;
    private LocalDateTime assignedDate;
}