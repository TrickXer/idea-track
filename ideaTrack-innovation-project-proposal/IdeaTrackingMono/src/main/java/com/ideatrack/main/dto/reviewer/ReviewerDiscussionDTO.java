package com.ideatrack.main.dto.reviewer;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewerDiscussionDTO {

    private Integer userActivityId;
    private Integer userId;
    private String displayName;
    private String commentText;
    private Integer stageId;
    private Integer replyParent;
    private LocalDateTime createdAt;
    
}