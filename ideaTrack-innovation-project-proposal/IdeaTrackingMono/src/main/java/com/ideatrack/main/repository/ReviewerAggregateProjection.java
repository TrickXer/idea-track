package com.ideatrack.main.repository;

public interface ReviewerAggregateProjection {

    Long getReviewerId();
    String getReviewerName();
    Integer getPendingTasks();
    Integer getOverdueByDays();
}
