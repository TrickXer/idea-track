package com.ideatrack.main.dto.idea.bulk;

import com.ideatrack.main.data.Constants;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Admin-selected bulk operations.
 * Provide at least one operation besides ideaIds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkIdeaOperationRequest {

    @NotEmpty
    private List<@NotNull Integer> ideaIds;

    // Operations (all optional; at least one must be provided)
    private Constants.IdeaStatus ideaStatus;

    private Integer categoryId;         // move to another category

    private Boolean delete;             // soft delete selected ideas

    private String tag;                 // set tag
    private Boolean clearTag;           // clear tag

    private String reviewerFeedback;    // set feedback
    private Boolean clearReviewerFeedback; // clear feedback

    private String thumbnailURL;        // set thumbnail
}
