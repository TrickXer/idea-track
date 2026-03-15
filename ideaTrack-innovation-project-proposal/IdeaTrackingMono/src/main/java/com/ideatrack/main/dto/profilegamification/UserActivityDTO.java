package com.ideatrack.main.dto.profilegamification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityDTO {
    private Integer id;
    private Integer userId;
    private Integer ideaId;
    private String commentText;
    private String voteType;       // enum mapped to string
    private Boolean savedIdea;
    private Integer delta;
    private Integer totalAfterChange;
    private String reason;
    private String activityType;   // enum mapped to string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}