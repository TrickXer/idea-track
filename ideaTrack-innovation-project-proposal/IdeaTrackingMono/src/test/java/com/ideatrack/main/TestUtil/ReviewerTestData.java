package com.ideatrack.main.TestUtil;

import com.ideatrack.main.data.*;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReviewerTestData {

    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    private ReviewerTestData() {}

    // ---------- IDs ----------
    public static int nextId() {
        return SEQ.incrementAndGet();
    }

    // ---------- Users ----------
    public static User reviewer(String name) {
        User u = new User();
        u.setUserId(nextId());
        u.setName(name);
        u.setRole(Constants.Role.REVIEWER);
        u.setDeleted(false);
        return u;
    }

    public static User employee(String name) {
        User u = new User();
        u.setUserId(nextId());
        u.setName(name);
        u.setRole(Constants.Role.REVIEWER);
        u.setDeleted(false);
        return u;
    }

    // ---------- Category ----------
    public static Category categoryWithStages(String name, int stageCount) {
        Category c = new Category();
        c.setCategoryId(nextId());
        c.setName(name);
        c.setStageCount(stageCount);
        c.setDeleted(false);
        return c;
    }

    // ---------- Idea ----------
    public static Idea ideaUnderReviewStage(Category category, User owner, int stage) {
        Idea i = new Idea();
        i.setIdeaId(nextId());
        i.setTitle("Improve internal workflow automation");
        i.setDeleted(false);
        i.setCategory(category);
        i.setUser(owner);
        i.setStage(stage);
        i.setIdeaStatus(Constants.IdeaStatus.UNDERREVIEW);
        return i;
    }

    // ---------- Assignment ----------
    public static AssignedReviewerToIdea assignment(Idea idea, Category category, User reviewer, int stage) {
        return AssignedReviewerToIdea.builder()
                .idea(idea)
                .category(category)
                .reviewer(reviewer)
                .stage(stage)
                .decision(null)
                .feedback(null)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();
    }
}
