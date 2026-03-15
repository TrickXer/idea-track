package com.ideatrack.main.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
	private Integer userActivityId;
    private Integer ideaId;
    private Integer userId;
    private String displayName;
    private String commentText;
    private LocalDateTime createdAt;
}