package com.ideatrack.main.dto.admin;

import lombok.Builder;

//Done by vibhuti

/**
 * Reviewer with overdue assignments info for admin insights.
 */
@Builder
public record AdminOverdueReviewerDto(
        Long reviewerId,
        Integer pendingTasks,
        Integer overdueByDays
) {}

