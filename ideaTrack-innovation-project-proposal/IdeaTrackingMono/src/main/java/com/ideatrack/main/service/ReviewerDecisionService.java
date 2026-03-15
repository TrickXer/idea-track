/**
 * Author - Pavan
 */
package com.ideatrack.main.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ideatrack.main.data.AssignedReviewerToIdea;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.data.UserActivity;
import com.ideatrack.main.dto.reviewer.ReviewerDecisionRequest;
import com.ideatrack.main.exception.ReviewerBadRequestException;
import com.ideatrack.main.exception.ReviewerForbiddenException;
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
public class ReviewerDecisionService {

    private static final String EVENT_REVIEWER_DECISION = "REVIEWER_DECISION";
    private static final String EVENT_STATUS_CHANGE = "STATUS_CHANGE";
    private static final String EVENT_STAGE_START = "STAGE_START";
    private static final String EVENT_MATRIX_OUTCOME = "MATRIX_OUTCOME";
    private static final String EVENT_MATRIX_OUTCOME_SLA = "MATRIX_OUTCOME_SLA";
    private static final String EVENT_DECISION_PENDING = "REVIEWER_DECISION_PENDING";
    private static final String EVENT_FINAL_DECISION = "FINAL_DECISION";

    private final IIdeaRepository ideaRepo;
    private final IAssignedReviewerToIdeaRepository reviewerAssignRepo;
    private final IUserActivityRepository activityRepo;
    private final IReviewerCategoryRepository reviewerCategoryRepo;
    private final IUserRepository userRepo;
    private final GamificationService gamificationService;
    private final NotificationHelper notificationHelper;

    public void processDecision(Integer ideaId, ReviewerDecisionRequest req) {
        if (ideaId == null) throw new ReviewerBadRequestException("REV_IDEAID_REQUIRED", "ideaId is required");
        if (req == null) throw new ReviewerBadRequestException("REV_BODY_REQUIRED", "request body is required");
        if (req.getDecision() == null || req.getDecision().isBlank())
            throw new ReviewerBadRequestException("REV_DECISION_REQUIRED", "decision is required");
        if (req.getFeedback() == null || req.getFeedback().isBlank()) {
            throw new ReviewerBadRequestException("REV_FEEDBACK_REQUIRED", "feedback is required");
        }
        if (req.getFeedback().length() > 2000) {
            throw new ReviewerBadRequestException("REV_FEEDBACK_TOO_LONG", "feedback must be <= 2000 chars");
        }

        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new ReviewerNotFoundException("REV_IDEA_NOT_FOUND", "Idea not found: " + ideaId));

        if (idea.isDeleted()) throw new ReviewerBadRequestException("REV_IDEA_DELETED", "Idea is deleted: " + ideaId);

        Integer stage = idea.getStage();
        if (stage == null) throw new ReviewerBadRequestException("REV_STAGE_NULL", "Idea stage is null");

        // Reviewer identity resolved from JWT — never trust client-supplied reviewerId
        Integer resolvedId = getAuthenticatedUserId();
        User reviewer = userRepo.findById(resolvedId)
                .orElseThrow(() -> new ReviewerNotFoundException("REV_USER_NOT_FOUND", "Authenticated user not found: " + resolvedId));

        if (reviewer.getRole() == null || reviewer.getRole()!=Constants.Role.REVIEWER) {
            throw new ReviewerForbiddenException("REV_ONLY_REVIEWER", "Only REVIEWER can submit decisions");
        }

        AssignedReviewerToIdea assignment = reviewerAssignRepo
                .findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(ideaId, reviewer.getUserId(), stage)
                .orElseThrow(() -> new ReviewerForbiddenException("REV_NOT_ASSIGNED", "Reviewer not assigned to current stage"));

        // If already timed-out, block update
        if (assignment.getDecision() != null && Constants.IdeaStatus.PENDING.name().equalsIgnoreCase(assignment.getDecision())) {
            throw new ReviewerForbiddenException("REV_TIMED_OUT", "Decision window expired. Already PENDING.");
        }

        // If the idea has been resubmitted (SUBMITTED or UNDERREVIEW) but the assignment row
        // still carries a stale decision from a previous REFINE cycle, clear it automatically.
        // This handles ideas that were in REFINE before our resetDecisionsForStage fix was deployed.
        boolean ideaResubmitted = idea.getIdeaStatus() == Constants.IdeaStatus.SUBMITTED
                || idea.getIdeaStatus() == Constants.IdeaStatus.UNDERREVIEW;
        if (ideaResubmitted && assignment.getDecision() != null && !assignment.getDecision().isBlank()) {
            assignment.setDecision(null);
            assignment.setFeedback(null);
            reviewerAssignRepo.save(assignment);
        }

        Constants.IdeaStatus decision = normalizeReviewerDecision(req.getDecision(), ideaId);

        // idempotent
        if (assignment.getDecision() != null && !assignment.getDecision().isBlank()) {

            // idempotent: same decision repeated -> ignore
            if (assignment.getDecision().equalsIgnoreCase(decision.name())) {
                return;
            }

            // block overwrite / change
            throw new ReviewerForbiddenException(
                    "REV_DECISION_LOCKED",
                    "Decision already submitted for this stage. Cannot change it."
            );
        }

        assignment.setDecision(decision.name());
        assignment.setFeedback(req.getFeedback());

        reviewerAssignRepo.save(assignment);

        // Notify idea owner: feedback posted by reviewer
        notificationHelper.notifyFeedbackPosted(idea, reviewer);

        ReviewerTimelineUtil.logCurrentStatus(activityRepo, gamificationService, idea, reviewer, stage,
                decision, EVENT_REVIEWER_DECISION, true, false);

        ensureStageStartMarker(idea, stage, reviewer);

        // resolve if all decided
        tryResolveStageIfReady(idea, stage, reviewer);
        ideaRepo.save(idea);

    }

    // ===================== Auth helper =====================
    private Integer getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new com.ideatrack.main.exception.UnauthorizedException("No authenticated user");
        }
        String username = auth.getName();
        return userRepo.findByEmail(username)
                .orElseThrow(() -> new com.ideatrack.main.exception.UserNotFoundException("Authenticated user not found: " + username))
                .getUserId();
    }

    // ===================== SLA expiry =====================

    @Scheduled(
            cron = "${reviewer.sla.cron:0 30 23 * * *}",   // 11:30 PM daily
            zone = "${app.tz:Asia/Kolkata}"
    )
    public void expireStageDecisionsBySla() {
        List<Idea> underReview = ideaRepo.findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus.UNDERREVIEW);
        LocalDateTime now = LocalDateTime.now();

        for (Idea idea : underReview) {
            if (idea == null || idea.isDeleted() || idea.getStage() == null) continue;

            Integer stage = idea.getStage();

            LocalDateTime stageStart = activityRepo
                    .findFirstByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalseOrderByCreatedAtAsc(
                            idea.getIdeaId(), stage, Constants.ActivityType.CURRENTSTATUS, EVENT_STAGE_START
                    )
                    .map(UserActivity::getCreatedAt)
                    .orElse(null);

            if (stageStart == null) {
                User owner = idea.getUser();
                if (owner != null) ensureStageStartMarker(idea, stage, owner);
                continue;
            }

            if (stageStart.plusDays(3).isAfter(now)) continue;

            List<AssignedReviewerToIdea> stageAssignments =
                    reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(idea.getIdeaId(), stage);

            boolean anyUpdated = false;

            for (AssignedReviewerToIdea a : stageAssignments) {
                if (a.getDecision() == null || a.getDecision().isBlank()) {
                    // ✅ mark pending
                    a.setDecision(Constants.IdeaStatus.PENDING.name());
                    // ✅ remove reviewer from this idea (soft delete)
                    a.setDeleted(true);
                    reviewerAssignRepo.save(a);

                    User reviewer = a.getReviewer();
                    if (reviewer != null) {
                        int pendingDelta = gamificationService.getDeltaForIdeaStatus(Constants.IdeaStatus.PENDING);
                        int penalty = pendingDelta == 0 ? 0 : -pendingDelta;

                        ReviewerTimelineUtil.logCurrentStatusWithDelta(activityRepo, gamificationService, idea, reviewer, stage,
                                Constants.IdeaStatus.PENDING, EVENT_DECISION_PENDING, penalty, true, false);

                        // Notify reviewer that their SLA expired
                        notificationHelper.notifyReviewerSlaExpired(idea, reviewer);
                    }
                    anyUpdated = true;
                }
            }

            if (anyUpdated) {
                User actor = idea.getUser();
                if (actor == null && !stageAssignments.isEmpty()) actor = stageAssignments.get(0).getReviewer();
                if (actor != null) {
                    resolveStageByMatrix(idea, stage, actor, true);
                    ideaRepo.save(idea);
                }
            }
        }
    }

    // ===================== Internal helpers =====================
    private Constants.IdeaStatus normalizeReviewerDecision(String raw, Integer ideaId) {
        String v = raw.trim().toUpperCase();

        if ("APPROVED".equals(v) || "APPROVE".equals(v)) {
            throw new ReviewerBadRequestException("REV_NO_APPROVED", "Use ACCEPTED not APPROVED in reviewer decisions");
        }
        if ("ACCEPT".equals(v)) v = "ACCEPTED";
        if ("REJECT".equals(v)) v = "REJECTED";

        Constants.IdeaStatus st;
        try {
            st = Constants.IdeaStatus.valueOf(v);
        } catch (IllegalArgumentException ex) {
            throw new ReviewerBadRequestException("REV_INVALID_DECISION", "Allowed: ACCEPTED/REJECTED/REFINE");
        }

        if (st != Constants.IdeaStatus.ACCEPTED && st != Constants.IdeaStatus.REJECTED && st != Constants.IdeaStatus.REFINE) {
            throw new ReviewerBadRequestException("REV_INVALID_DECISION", "Allowed: ACCEPTED/REJECTED/REFINE");
        }

        // Only block REFINE if it has already been used as a *resolved matrix outcome*
        // (MATRIX_OUTCOME or MATRIX_OUTCOME_SLA). Individual REVIEWER_DECISION rows must NOT
        // be counted here — they are pre-outcome votes and are written before resolution.
        boolean refineUsed = activityRepo.existsByIdea_IdeaIdAndActivityTypeAndDecisionAndEventInAndDeletedFalse(
                ideaId, Constants.ActivityType.CURRENTSTATUS, Constants.IdeaStatus.REFINE,
                java.util.List.of(EVENT_MATRIX_OUTCOME, EVENT_MATRIX_OUTCOME_SLA));

        if (refineUsed && st == Constants.IdeaStatus.REFINE) {
            throw new ReviewerBadRequestException("REV_REFINE_USED", "REFINE is allowed only once per idea");
        }

        return st;
    }

    private void ensureStageStartMarker(Idea idea, Integer stage, User actor) {
        boolean exists = activityRepo.existsByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalse(
                idea.getIdeaId(), stage, Constants.ActivityType.CURRENTSTATUS, EVENT_STAGE_START);
        if (!exists) {
            ReviewerTimelineUtil.logCurrentStatus(activityRepo, gamificationService, idea, actor, stage,
                    Constants.IdeaStatus.UNDERREVIEW, EVENT_STAGE_START, false, false);
        }
    }

    private void tryResolveStageIfReady(Idea idea, Integer stage, User actor) {
        List<AssignedReviewerToIdea> stageAssignments =
                reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(idea.getIdeaId(), stage);

        boolean allDecided = stageAssignments.stream().allMatch(a -> a.getDecision() != null && !a.getDecision().isBlank());
        if (!allDecided) return;

        resolveStageByMatrix(idea, stage, actor, false);
    }

    // ===================== Matrix resolve & persist policy (REPLACED) =====================
    private void resolveStageByMatrix(Idea idea, Integer stage, User actor, boolean slaTriggered) {
        List<AssignedReviewerToIdea> stageAssignments =
                reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(idea.getIdeaId(), stage);

        long accepted = stageAssignments.stream()
                .map(a -> a.getDecision() == null ? "" : a.getDecision().trim().toUpperCase())
                .filter(d -> d.equals("ACCEPTED") || d.equals("APPROVED")) // robust to legacy APPROVED
                .count();
        long rejected = stageAssignments.stream()
                .map(a -> a.getDecision() == null ? "" : a.getDecision().trim().toUpperCase())
                .filter(d -> d.equals("REJECTED"))
                .count();
        long refine = stageAssignments.stream()
                .map(a -> a.getDecision() == null ? "" : a.getDecision().trim().toUpperCase())
                .filter(d -> d.equals("REFINE"))
                .count();

        long votes = accepted + rejected + refine;
        if (votes == 0) return;

        // Only consider REFINE "used" when a previous *matrix outcome* logged REFINE
        // (event = MATRIX_OUTCOME or MATRIX_OUTCOME_SLA). Individual reviewer votes
        // (event = REVIEWER_DECISION) must NOT count — they are written before this
        // method runs and would otherwise falsely set refineUsed=true, causing a
        // single REFINE vote to resolve as REJECTED instead of REFINE.
        boolean refineUsed = activityRepo.existsByIdea_IdeaIdAndActivityTypeAndDecisionAndEventInAndDeletedFalse(
                idea.getIdeaId(), Constants.ActivityType.CURRENTSTATUS, Constants.IdeaStatus.REFINE,
                java.util.List.of(EVENT_MATRIX_OUTCOME, EVENT_MATRIX_OUTCOME_SLA));

        Constants.IdeaStatus outcome;
        if (refineUsed) {
            // tie -> reject; majority wins otherwise
            outcome = (accepted > rejected) ? Constants.IdeaStatus.ACCEPTED : Constants.IdeaStatus.REJECTED;
        } else {
            // majority wins, tie -> refine
            if (accepted > rejected && accepted > refine) outcome = Constants.IdeaStatus.ACCEPTED;
            else if (rejected > accepted && rejected > refine) outcome = Constants.IdeaStatus.REJECTED;
            else outcome = Constants.IdeaStatus.REFINE;
        }

        // ----- DO NOT set idea.ideaStatus = outcome here (to avoid intermediate writes) -----

        // Log matrix outcome to UserActivity (UNCHANGED)
        ReviewerTimelineUtil.logCurrentStatus(
                activityRepo, gamificationService, idea, actor, stage,
                outcome,
                slaTriggered ? EVENT_MATRIX_OUTCOME_SLA : EVENT_MATRIX_OUTCOME,
                true,  // XP to actor
                true   // XP to owner
        );

        // Apply Idea-table policy
        if (outcome == Constants.IdeaStatus.REFINE) {
            // REFINE anywhere -> write REFINE + mirror feedback
            mirrorStageFeedbackToIdea(idea, stage);
            idea.setIdeaStatus(Constants.IdeaStatus.REFINE);
            ideaRepo.save(idea);
            notificationHelper.notifyIdeaRefine(idea, actor);
            return;
        }

        if (outcome == Constants.IdeaStatus.REJECTED) {
            // REJECTED final at any stage -> write + mirror feedback + final log
            mirrorStageFeedbackToIdea(idea, stage);
            idea.setIdeaStatus(Constants.IdeaStatus.REJECTED);
            ideaRepo.save(idea);

            ReviewerTimelineUtil.logFinalDecision(
                    activityRepo, gamificationService, idea, actor, stage,
                    Constants.IdeaStatus.REJECTED, EVENT_FINAL_DECISION, true, true
            );
            notificationHelper.notifyIdeaRejected(idea, actor);
            return;
        }

        // ACCEPTED path
        Integer nextStage =
                (idea.getCategory() != null && idea.getCategory().getStageCount() > stage)
                        ? stage + 1 : null;

        if (nextStage != null) {
            // Intermediate ACCEPTED -> advance stage ONLY; do not write to Idea table
            idea.setStage(nextStage);
            ideaRepo.save(idea);

            // Keep workflow timeline (UNCHANGED)
            ReviewerTimelineUtil.logCurrentStatus(
                    activityRepo, gamificationService, idea, actor, nextStage,
                    Constants.IdeaStatus.UNDERREVIEW, EVENT_STATUS_CHANGE, true, true
            );

            // Assign next-stage reviewers (UNCHANGED)
            List<Integer> ids = reviewerCategoryRepo.findActiveReviewerUserIdsByCategoryAndStage(
                    idea.getCategory().getCategoryId(), nextStage);
            if (ids != null && !ids.isEmpty()) {
                List<User> users = userRepo.findAllById(ids);
                for (User u : users) {
                    boolean ever = reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStage(
                            idea.getIdeaId(), u.getUserId(), nextStage);
                    if (!ever) {
                        reviewerAssignRepo.save(AssignedReviewerToIdea.builder()
                                .idea(idea).reviewer(u).category(idea.getCategory())
                                .stage(nextStage).deleted(false).build());
                        // Notify each newly-assigned reviewer
                        notificationHelper.notifyReviewerAssigned(idea, u, nextStage);
                    }
                }
            }
            // Notify idea owner of stage advancement
            notificationHelper.notifyIdeaStageAdvanced(idea, actor, nextStage);
            ensureStageStartMarker(idea, nextStage, actor);
            return;
        }

        // Final stage ACCEPTED -> write ACCEPTED + mirror final-stage feedback + final logs
        mirrorStageFeedbackToIdea(idea, stage);
        idea.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);
        ideaRepo.save(idea);

        ReviewerTimelineUtil.logCurrentStatus(
                activityRepo, gamificationService, idea, actor, stage,
                Constants.IdeaStatus.ACCEPTED, EVENT_STATUS_CHANGE, true, true
        );
        ReviewerTimelineUtil.logFinalDecision(
                activityRepo, gamificationService, idea, actor, stage,
                Constants.IdeaStatus.ACCEPTED, EVENT_FINAL_DECISION, true, true
        );
        notificationHelper.notifyIdeaAccepted(idea, actor);
    }

    public List<ReviewerDecisionRequest> getReviewerDecisions(Integer ideaId) {
        if (ideaId == null) {
            throw new ReviewerBadRequestException("REV_IDEAID_REQUIRED", "ideaId is required");
        }

        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new ReviewerNotFoundException("REV_IDEA_NOT_FOUND", "Idea not found: " + ideaId));

        if (idea.isDeleted()) {
            throw new ReviewerBadRequestException("REV_IDEA_DELETED", "Idea is deleted: " + ideaId);
        }

        return reviewerAssignRepo.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(ideaId)
                .stream()
                // include only rows where decision exists; remove filter if you want UNDERREVIEW items too
                .filter(a -> a.getDecision() != null && !a.getDecision().isBlank())
                .map(a -> {
                    ReviewerDecisionRequest dto = new ReviewerDecisionRequest();
                    dto.setReviewerId(a.getReviewer() != null ? a.getReviewer().getUserId() : null);
                    dto.setDecision(a.getDecision());
                    dto.setFeedback(a.getFeedback());
                    return dto;
                })
                .toList();
    }

    // --- Helper: mirrors one non-blank reviewer feedback for the resolved stage into idea.reviewerFeedback ---
    /**
     * Mirrors any one non-blank reviewer feedback of the resolved stage into idea.reviewerFeedback.
     * Called right after stage outcome is decided for `stage`.
     */
    private void mirrorStageFeedbackToIdea(Idea idea, Integer resolvedStage) {
        if (idea == null || resolvedStage == null) return;

        // Get all assignments for this idea & stage (non-deleted)
        List<com.ideatrack.main.data.AssignedReviewerToIdea> rows =
                reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(idea.getIdeaId(), resolvedStage);

        if (rows == null || rows.isEmpty()) return;

        // Pick any one non-blank feedback. Prefer the "latest by updatedAt" if present; else any non-blank.
        String chosen = rows.stream()
                .filter(r -> r.getFeedback() != null && !r.getFeedback().isBlank())
                .sorted((a, b) -> {
                    var au = a.getUpdatedAt();
                    var bu = b.getUpdatedAt();
                    if (au == null && bu == null) return 0;
                    if (au == null) return 1;
                    if (bu == null) return -1;
                    return bu.compareTo(au); // newest first
                })
                .map(com.ideatrack.main.data.AssignedReviewerToIdea::getFeedback)
                .findFirst()
                .orElse(null);

        if (chosen == null) return;

        // Only update if changed to avoid unnecessary writes
        if (!chosen.equals(idea.getReviewerFeedback())) {
            idea.setReviewerFeedback(chosen);
            ideaRepo.save(idea); // will also bump updatedAt by auditing
        }
    }
}