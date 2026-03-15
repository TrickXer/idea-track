/**
 Author - Pavan
 */
package com.ideatrack.main.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ideatrack.main.data.AssignedReviewerToIdea;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.exception.ReviewerNotFoundException;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IReviewerCategoryRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewerAssignmentService {

    private static final String EVENT_STATUS_CHANGE = "STATUS_CHANGE";
    private static final String EVENT_STAGE_START = "STAGE_START";

    private final IIdeaRepository ideaRepo;
    private final IAssignedReviewerToIdeaRepository reviewerAssignRepo;
    private final IUserActivityRepository activityRepo;
    private final IReviewerCategoryRepository reviewerCategoryRepo;
    private final IUserRepository userRepo;
    private final GamificationService gamificationService;
    private final NotificationHelper notificationHelper;

    // Run once per day (end of day)
 // ReviewerAssignmentService.java

    @Scheduled(
        cron = "${reviewer.assign.cron:0 59 23 * * *}"
    )
    public void assignSubmittedIdeasEndOfDay() {

        // ✅ Directly fetch ideas that are currently SUBMITTED from Idea table
        List<Idea> submitted = ideaRepo.findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus.SUBMITTED);
        if (submitted == null || submitted.isEmpty()) {
            log.info("EOD reviewer assignment: no ideas found in SUBMITTED.");
            return;
        }

        int processed = 0;
        for (Idea idea : submitted) {
            if (idea == null || idea.isDeleted() || idea.getCategory() == null) continue;

            Integer stage = 1; // first review stage
            Integer categoryId = idea.getCategory().getCategoryId();

            // Find active reviewers for category + stage
            List<Integer> reviewerIds =
                    reviewerCategoryRepo.findActiveReviewerUserIdsByCategoryAndStage(categoryId, stage);

            if (reviewerIds == null || reviewerIds.isEmpty()) {
                // No reviewers mapped — keep idea in SUBMITTED
                log.info("No active reviewers for ideaId={} categoryId={} stage={}", idea.getIdeaId(), categoryId, stage);
                continue;
            }

            List<User> reviewers = userRepo.findAllById(reviewerIds);
            for (User r : reviewers) {
                if (r == null) continue;

                // Prevent re-assign if ever assigned before (even if later soft-deleted)
                boolean everAssigned = reviewerAssignRepo
                        .existsByIdea_IdeaIdAndReviewer_UserIdAndStage(idea.getIdeaId(), r.getUserId(), stage);
                if (everAssigned) continue;

                reviewerAssignRepo.save(com.ideatrack.main.data.AssignedReviewerToIdea.builder()
                        .idea(idea)
                        .reviewer(r)
                        .category(idea.getCategory())
                        .stage(stage)
                        .decision(null)
                        .deleted(false)
                        .build());

                // Notify the reviewer of the new assignment
                notificationHelper.notifyReviewerAssigned(idea, r, stage);
            }

            // Move status to UNDERREVIEW (and keep stage=1)
            idea.setStage(stage);
            idea.setIdeaStatus(Constants.IdeaStatus.UNDERREVIEW);
            ideaRepo.save(idea);

            User owner = idea.getUser();
            if (owner == null) {
                log.warn("Idea owner not found for ideaId={}. Skipping timeline log.", idea.getIdeaId());
                continue;
            }

            // Log status change + stage start (same as earlier)
            ReviewerTimelineUtil.logCurrentStatus(
                    activityRepo, gamificationService, idea, owner, stage,
                    Constants.IdeaStatus.UNDERREVIEW, "STATUS_CHANGE", true, true);

            ReviewerTimelineUtil.logCurrentStatus(
                    activityRepo, gamificationService, idea, owner, stage,
                    Constants.IdeaStatus.UNDERREVIEW, "STAGE_START", false, false);

            processed++;
            log.info("EOD Auto-assigned ideaId={} stage={} reviewers={}", idea.getIdeaId(), stage, reviewers.size());
        }

        log.info("EOD reviewer assignment completed. processed={}/{}", processed, submitted.size());
    }
}