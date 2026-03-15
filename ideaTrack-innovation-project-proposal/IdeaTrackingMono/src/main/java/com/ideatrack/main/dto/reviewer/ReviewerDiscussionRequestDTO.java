package com.ideatrack.main.dto.reviewer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class ReviewerDiscussionRequestDTO {

    /** User posting the comment. Must be an assigned REVIEWER for current stage. */
    @NotNull(message = "userId is required")
    @Positive(message = "userId must be positive")
    private Integer userId;

    /** Stage where discussion belongs. Must match idea.stage (current stage only). */
    @NotNull(message = "stageId is required")
    @Min(value = 1, message = "stageId must be >= 1")
    private Integer stageId;

    /** Comment text. */
    @NotBlank(message = "text is required")
    @Size(max = 2000, message = "text must be <= 2000 characters")
    private String text;

    /** Parent comment activity id if reply; null for top-level comment. */
    @Positive(message = "replyParent must be positive")
    private Integer replyParent;
}