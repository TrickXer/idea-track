package com.ideatrack.main.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ideatrack.main.data.AssignedReviewerToIdea;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.data.UserActivity;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.dto.PagedResponse;
import com.ideatrack.main.dto.reviewer.ReviewerDashboardDTO;
import com.ideatrack.main.dto.reviewer.ReviewerDecisionRequest;
import com.ideatrack.main.dto.reviewer.ReviewerDiscussionDTO;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IReviewerCategoryRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewerService {

	private final IIdeaRepository ideaRepo;
	private final IAssignedReviewerToIdeaRepository reviewerAssignRepo;
	private final IUserActivityRepository activityRepo;
	private final IReviewerCategoryRepository reviewerCategoryRepo;
	private final IUserRepository userRepo;
	private final GamificationService gamificationService;

    // ==============================
    // ✅ Constants / Events
    // ==============================
    private static final String EVENT_REFINE_ACTION = "REFINE_ACTION";
    private static final String EVENT_FOLLOWUP = "FOLLOWUP";

    // CURRENTSTATUS timeline events
    private static final String EVENT_REVIEWER_DECISION = "REVIEWER_DECISION";
    private static final String EVENT_STATUS_CHANGE = "STATUS_CHANGE";
    private static final String EVENT_STAGE_START = "STAGE_START";
    private static final String EVENT_MATRIX_OUTCOME = "MATRIX_OUTCOME";
    private static final String EVENT_MATRIX_OUTCOME_SLA = "MATRIX_OUTCOME_SLA";
    private static final String EVENT_DECISION_PENDING = "REVIEWER_DECISION_PENDING";

    // FINALDECISION timeline events (whole idea)
    private static final String EVENT_FINAL_DECISION = "FINAL_DECISION";

    // ==============================
    // ✅ 1) Process Reviewer Decision (ACCEPTED/REJECTED/REFINE only)
    // ==============================
    public void processDecision(Integer ideaId,Integer authenticatedReviewerId, ReviewerDecisionRequest req) {
        validateDecisionRequest(ideaId, req);

        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new EntityNotFoundException("Idea not found: " + ideaId));

        if (idea.isDeleted()) {
            throw new IllegalArgumentException("Idea is deleted: " + ideaId);
        }

        Integer currentStage = idea.getStage();
        if (currentStage == null) {
            throw new IllegalStateException("Idea stage is null");
        }

        User reviewer = userRepo.findById(req.getReviewerId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + req.getReviewerId()));

        // ✅ Updated to enum-based role check
        if (reviewer.getRole() == null || reviewer.getRole() != Constants.Role.REVIEWER) {
            throw new IllegalArgumentException("Only REVIEWER can submit decisions");
        }

        // ✅ Strict assignment at CURRENT stage only
        AssignedReviewerToIdea assignment = reviewerAssignRepo
                .findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(ideaId, req.getReviewerId(), currentStage)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reviewer not assigned to this idea at current stage"));

        // ✅ Only ACCEPTED / REJECTED / REFINE
        Constants.IdeaStatus decision = normalizeReviewerDecision(req.getDecision());

        // ✅ One-time refine enforced by timeline marker (CURRENTSTATUS decision=REFINE exists)
        boolean refineUsed = activityRepo.existsByIdea_IdeaIdAndActivityTypeAndDecisionAndDeletedFalse(
                ideaId, Constants.ActivityType.CURRENTSTATUS, Constants.IdeaStatus.REFINE
        );
        if (refineUsed && decision == Constants.IdeaStatus.REFINE) {
            throw new IllegalArgumentException("REFINE is allowed only once per idea");
        }

        // idempotent decision save
        String prev = assignment.getDecision();
        String newVal = decision.name();
        if (prev != null && prev.equalsIgnoreCase(newVal)) {
            log.info("Duplicate decision ignored: ideaId={}, reviewerId={}, stage={}, decision={}",
                    ideaId, req.getReviewerId(), currentStage, newVal);
            return;
        }

        assignment.setDecision(newVal);
        assignment.setFeedback(req.getFeedback());
        reviewerAssignRepo.save(assignment);

        // ✅ Timeline: reviewer decision stored in CURRENTSTATUS (decision column stores ACCEPTED/REJECTED/REFINE/PENDING)
        logCurrentStatusActivity(
                idea,
                reviewer,
                currentStage,
                decision,
                EVENT_REVIEWER_DECISION,
                true,   // XP to reviewer (based on getDeltaForIdeaStatus)
                false   // do not give owner XP for each individual reviewer decision
        );

        // Ensure idea is under review (status/timeline) if it is still submitted/pending
        if (idea.getIdeaStatus() == Constants.IdeaStatus.SUBMITTED || idea.getIdeaStatus() == Constants.IdeaStatus.PENDING) {
            idea.setIdeaStatus(Constants.IdeaStatus.UNDERREVIEW);
            logCurrentStatusActivity(idea, reviewer, currentStage,
                    Constants.IdeaStatus.UNDERREVIEW, EVENT_STATUS_CHANGE, true, true);
            // Stage start marker for SLA
            ensureStageStartMarker(idea, currentStage, reviewer);
        }

        // Resolve stage if all decided (PENDING counts as decided if stored)
        tryResolveStageIfReady(idea, currentStage, reviewer);

        ideaRepo.save(idea);
    }

    // ==============================
    // ✅ 2) Threaded Discussion (CURRENT STAGE ONLY + assigned reviewers only)
    // ==============================
    public void postDiscussion(Integer ideaId, Integer userId, Integer stageId, String text, Integer replyParent) {
        validateDiscussionRequest(ideaId, userId, stageId, text);

        final User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        final Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new EntityNotFoundException("Idea not found: " + ideaId));

        if (idea.isDeleted()) {
            throw new IllegalArgumentException("Idea is deleted: " + ideaId);
        }

        // ✅ CURRENT STAGE ONLY
        if (!Objects.equals(idea.getStage(), stageId)) {
            throw new IllegalArgumentException("Discussion allowed only for current stage. ideaStage=" + idea.getStage());
        }

        // ✅ REVIEWER ONLY (updated to enum-based check)
        if (user.getRole() == null || user.getRole() != Constants.Role.REVIEWER) {
            throw new IllegalArgumentException("Only REVIEWER can post reviewer discussions");
        }

        // ✅ MUST BE ASSIGNED to this IDEA + STAGE
        boolean assigned = reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
                ideaId, userId, stageId
        );
        if (!assigned) {
            throw new IllegalArgumentException("Reviewer not assigned to this idea/stage");
        }

        // ✅ replyParent safety
        final UserActivity parentComment = resolveAndValidateParent(replyParent, ideaId, stageId);

        int delta = gamificationService.getDeltaForActivity(Constants.ActivityType.REVIEWDISCUSSION);

        UserActivity discussion = UserActivity.builder()
                .idea(idea)
                .user(user)
                .commentText(text)
                .activityType(Constants.ActivityType.REVIEWDISCUSSION)
                .event(EVENT_FOLLOWUP)
                .stageId(stageId)
                .replyParent(parentComment)
                .delta(delta)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        activityRepo.save(discussion);

        if (delta != 0) {
            gamificationService.applyDelta(userId, delta);
        }

        log.debug("Discussion posted: ideaId={}, stageId={}, userId={}, parent={}",
                ideaId, stageId, userId, replyParent);
    }

    // ==============================
    // ✅ 3) Fetch Discussions (existing)
    // ==============================
    public List<ReviewerDiscussionDTO> getDiscussionsForStage(Integer ideaId, Integer stageId) {
        return activityRepo.findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalseOrderByCreatedAtAsc(
                        ideaId, Constants.ActivityType.REVIEWDISCUSSION, stageId)
                .stream()
                .map(this::toDiscussionDTO)
                .toList();
    }

    // ==============================
    // ✅ 4) Decision History (from assignment table, not UA)
    // ==============================
    public List<ReviewerDecisionRequest> getReviewerDecisions(Integer ideaId) {
        List<AssignedReviewerToIdea> assignments = reviewerAssignRepo
                .findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(ideaId);

        return assignments.stream()
                .filter(a -> a.getDecision() != null && !a.getDecision().isBlank())
                .sorted(Comparator
                        .comparing(AssignedReviewerToIdea::getStage, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AssignedReviewerToIdea::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                )
                .map(a -> {
                    ReviewerDecisionRequest dto = new ReviewerDecisionRequest();
                    dto.setReviewerId(a.getReviewer() != null ? a.getReviewer().getUserId() : null);
                    dto.setDecision(a.getDecision());
                    dto.setFeedback(a.getFeedback());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ==============================
    // ✅ 5) Reviewer Dashboard
    // ==============================
    public List<ReviewerDashboardDTO> getReviewerDashboard(Integer reviewerId, String filter) {
        String decisionFilter = null;
        if (filter != null && !filter.equalsIgnoreCase("ALL")) {
            decisionFilter = normalizeDashboardDecisionFilter(filter);
        }

        List<AssignedReviewerToIdea> assignments = reviewerAssignRepo.findDashboardIdeas(reviewerId, decisionFilter);

        return assignments.stream()
                .map(this::toDashboardDTO)
                .toList();
    }

    // ==========================================================
    // ✅ NEW PAGED METHODS (existing pattern)
    // ==========================================================
    public PagedResponse<ReviewerDiscussionDTO> getDiscussionsForStagePaged(
            Integer ideaId, Integer stageId, Pageable pageable) {

        Page<UserActivity> page = activityRepo.findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalse(
                ideaId, Constants.ActivityType.REVIEWDISCUSSION, stageId, pageable);

        Page<ReviewerDiscussionDTO> mapped = page.map(this::toDiscussionDTO);
        return buildPagedResponse(mapped);
    }

    public PagedResponse<ReviewerDecisionRequest> getReviewerDecisionsPaged(Integer ideaId, Pageable pageable) {
        Page<AssignedReviewerToIdea> page = reviewerAssignRepo
                .findByIdea_IdeaIdAndDeletedFalseAndDecisionIsNotNull(ideaId, pageable);

        Page<ReviewerDecisionRequest> mapped = page.map(a -> {
            ReviewerDecisionRequest dto = new ReviewerDecisionRequest();
            dto.setReviewerId(a.getReviewer() != null ? a.getReviewer().getUserId() : null);
            dto.setDecision(a.getDecision());
            dto.setFeedback(a.getFeedback());
            return dto;
        });

        return buildPagedResponse(mapped);
    }

    public PagedResponse<ReviewerDashboardDTO> getReviewerDashboardPaged(
            Integer reviewerId, String filter, Pageable pageable) {

        String decisionFilter = null;
        if (filter != null && !filter.equalsIgnoreCase("ALL")) {
            decisionFilter = normalizeDashboardDecisionFilter(filter);
        }

        Page<AssignedReviewerToIdea> page = reviewerAssignRepo.findDashboardIdeasPaged(reviewerId, decisionFilter, pageable);
        Page<ReviewerDashboardDTO> mapped = page.map(this::toDashboardDTO);

        return buildPagedResponse(mapped);
    }

    // ==========================================================
    // ✅ SCHEDULERS (Auto assign + SLA expiry)
    // ==========================================================

    /**
     * Auto-assign SUBMITTED ideas to reviewers (category+stage=1).
     * If no reviewers exist -> keep idea in SUBMITTED.
     *
     * NOTE: Requires repo append method:
     * - List<Integer> findReviewerIdsForCategoryStage(Integer categoryId, Integer stage)
     */
    @Scheduled(fixedDelayString = "${reviewer.assign.scan.ms:60000}")
    public void assignSubmittedIdeasAutomatically() {
        List<Idea> submitted = ideaRepo.findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus.SUBMITTED);
        for (Idea idea : submitted) {
            if (idea == null || idea.isDeleted()) continue;
            if (idea.getCategory() == null) continue;

            Integer categoryId = idea.getCategory().getCategoryId();
            Integer stage = 1;

            List<Integer> reviewerIds =
                    reviewerCategoryRepo.findActiveReviewerUserIdsByCategoryAndStage(categoryId, stage);
            if (reviewerIds == null || reviewerIds.isEmpty()) {
                // no reviewers -> remain SUBMITTED
                continue;
            }

            List<User> reviewers = userRepo.findAllById(reviewerIds);
            if (reviewers.isEmpty()) continue;

            for (User r : reviewers) {
                if (r == null) continue;
                boolean exists = reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
                        idea.getIdeaId(), r.getUserId(), stage
                );
                if (!exists) {
                    AssignedReviewerToIdea a = new AssignedReviewerToIdea();
                    a.setIdea(idea);
                    a.setReviewer(r);
                    a.setStage(stage);
                    a.setCategory(idea.getCategory());
                    a.setDeleted(false);
                    reviewerAssignRepo.save(a);
                }
            }

            // Move idea to UNDERREVIEW stage 1
            idea.setStage(stage);
            idea.setIdeaStatus(Constants.IdeaStatus.UNDERREVIEW);
            ideaRepo.save(idea);

            // Timeline logs
            User owner = idea.getUser();
            if (owner != null) {
                // avoid duplicate submitted marker
                if (!activityRepo.existsByIdea_IdeaIdAndActivityTypeAndDecisionAndDeletedFalse(
                        idea.getIdeaId(), Constants.ActivityType.CURRENTSTATUS, Constants.IdeaStatus.SUBMITTED)) {
                    logCurrentStatusActivity(idea, owner, stage, Constants.IdeaStatus.SUBMITTED, EVENT_STATUS_CHANGE, true, true);
                }
                logCurrentStatusActivity(idea, owner, stage, Constants.IdeaStatus.UNDERREVIEW, EVENT_STATUS_CHANGE, true, true);
                ensureStageStartMarker(idea, stage, owner);
            }

            log.info("Auto assigned ideaId={} to {} reviewers (stage 1)", idea.getIdeaId(), reviewers.size());
        }
    }

    /**
     * SLA expiry scan (3 days). If reviewer didn't decide in time:
     * - mark assignment decision = PENDING
     * - write CURRENTSTATUS timeline decision=PENDING with NEGATIVE delta (penalty)
     * - resolve matrix with remaining votes (PENDING ignored in counts).
     *
     * NOTE: Requires repo append method:
     * - Optional<UserActivity> findFirstByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalseOrderByCreatedAtAsc(...)
     */
    @Scheduled(fixedDelayString = "${reviewer.sla.scan.ms:60000}")
    public void expireStageDecisionsBySla() {
        List<Idea> underReview = ideaRepo.findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus.UNDERREVIEW);
        LocalDateTime now = LocalDateTime.now();

        for (Idea idea : underReview) {
            if (idea == null || idea.isDeleted()) continue;
            Integer stage = idea.getStage();
            if (stage == null) continue;

            LocalDateTime stageStart = activityRepo
                    .findFirstByIdea_IdeaIdAndStageIdAndActivityTypeAndEventAndDeletedFalseOrderByCreatedAtAsc(
                            idea.getIdeaId(), stage, Constants.ActivityType.CURRENTSTATUS, EVENT_STAGE_START
                    )
                    .map(UserActivity::getCreatedAt)
                    .orElse(null);

            if (stageStart == null) {
                // create marker if missing (safe default)
                User owner = idea.getUser();
                if (owner != null) {
                    ensureStageStartMarker(idea, stage, owner);
                }
                continue;
            }

            if (stageStart.plusDays(3).isAfter(now)) {
                continue; // still within SLA
            }

            // SLA expired -> mark pending for undecided
            List<AssignedReviewerToIdea> stageAssignments =
                    reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(idea.getIdeaId(), stage);

            boolean anyUpdated = false;

            for (AssignedReviewerToIdea a : stageAssignments) {
                if (a.getDecision() == null || a.getDecision().isBlank()) {
                    a.setDecision(Constants.IdeaStatus.PENDING.name());
                    reviewerAssignRepo.save(a);

                    User reviewer = a.getReviewer();
                    if (reviewer != null) {
                        // penalty delta = -pendingDelta
                        int pendingDelta = gamificationService.getDeltaForIdeaStatus(Constants.IdeaStatus.PENDING);
                        int penalty = pendingDelta == 0 ? 0 : -pendingDelta;

                        logCurrentStatusActivityWithDelta(
                                idea, reviewer, stage,
                                Constants.IdeaStatus.PENDING,
                                EVENT_DECISION_PENDING,
                                penalty,
                                true,  // apply to reviewer
                                false
                        );
                    }
                    anyUpdated = true;
                }
            }

            if (anyUpdated) {
                // resolve with SLA-triggered matrix
                User actor = idea.getUser();
                if (actor == null && !stageAssignments.isEmpty()) {
                    actor = stageAssignments.get(0).getReviewer();
                }
                if (actor != null) {
                    resolveStageByMatrix(idea, stage, actor, true);
                    ideaRepo.save(idea);
                }
            }
        }
    }

    // ==========================================================
    // ✅ MATRIX LOGIC (Majority + tie->REFINE, refineUsed tie->REJECTED)
    // ==========================================================

    private void tryResolveStageIfReady(Idea idea, Integer stage, User actor) {
        List<AssignedReviewerToIdea> stageAssignments =
                reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(idea.getIdeaId(), stage);

        if (stageAssignments.isEmpty()) {
            throw new IllegalStateException("No assignments found for ideaId=" + idea.getIdeaId() + ", stage=" + stage);
        }

        boolean allDecided = stageAssignments.stream()
                .allMatch(a -> a.getDecision() != null && !a.getDecision().isBlank());

        if (!allDecided) return;

        resolveStageByMatrix(idea, stage, actor, false);
    }

    private void resolveStageByMatrix(Idea idea, Integer stage, User actor, boolean triggeredBySlaExpiry) {
        List<AssignedReviewerToIdea> stageAssignments =
                reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(idea.getIdeaId(), stage);

        long accepted = stageAssignments.stream().filter(a -> "ACCEPTED".equalsIgnoreCase(a.getDecision())).count();
        long rejected = stageAssignments.stream().filter(a -> "REJECTED".equalsIgnoreCase(a.getDecision())).count();
        long refine   = stageAssignments.stream().filter(a -> "REFINE".equalsIgnoreCase(a.getDecision())).count();
        // PENDING is ignored in vote counts

        boolean refineUsed = activityRepo.existsByIdea_IdeaIdAndActivityTypeAndDecisionAndDeletedFalse(
                idea.getIdeaId(), Constants.ActivityType.CURRENTSTATUS, Constants.IdeaStatus.REFINE
        );

        Constants.IdeaStatus outcome;

        if (refineUsed) {
            // only ACCEPTED/REJECTED allowed, tie => REJECTED
            if (accepted > rejected) outcome = Constants.IdeaStatus.ACCEPTED;
            else if (rejected > accepted) outcome = Constants.IdeaStatus.REJECTED;
            else outcome = Constants.IdeaStatus.REJECTED;
        } else {
            // majority wins, any tie => REFINE
            if (accepted > rejected && accepted > refine) outcome = Constants.IdeaStatus.ACCEPTED;
            else if (rejected > accepted && rejected > refine) outcome = Constants.IdeaStatus.REJECTED;
            else if (refine > accepted && refine > rejected) outcome = Constants.IdeaStatus.REFINE;
            else outcome = Constants.IdeaStatus.REFINE; // tie -> refine
        }

        // Apply outcome to idea status
        idea.setIdeaStatus(outcome);

        logCurrentStatusActivity(
                idea, actor, stage, outcome,
                triggeredBySlaExpiry ? EVENT_MATRIX_OUTCOME_SLA : EVENT_MATRIX_OUTCOME,
                true, true
        );

        if (outcome == Constants.IdeaStatus.REFINE) {
            // mark refine used via CURRENTSTATUS row itself (already done above)
            // remains in same stage; employee will fix and resubmit
            return;
        }

        if (outcome == Constants.IdeaStatus.REJECTED) {
            // Whole idea final decision marker
            logFinalDecisionActivity(idea, actor, stage, Constants.IdeaStatus.REJECTED, EVENT_FINAL_DECISION, true, true);
            return;
        }

        // outcome ACCEPTED -> next stage or final APPROVED
        Integer nextStage = findNextStageFromAssignments(idea.getIdeaId(), stage);
        if (nextStage != null) {
            idea.setStage(nextStage);
            idea.setIdeaStatus(Constants.IdeaStatus.UNDERREVIEW);
            logCurrentStatusActivity(idea, actor, nextStage, Constants.IdeaStatus.UNDERREVIEW, EVENT_STATUS_CHANGE, true, true);
            ensureStageStartMarker(idea, nextStage, actor);
        } else {
            // final whole idea status becomes APPROVED (as you requested for whole idea final)
            idea.setIdeaStatus(Constants.IdeaStatus.APPROVED);
            logCurrentStatusActivity(idea, actor, stage, Constants.IdeaStatus.APPROVED, EVENT_STATUS_CHANGE, true, true);
            logFinalDecisionActivity(idea, actor, stage, Constants.IdeaStatus.APPROVED, EVENT_FINAL_DECISION, true, true);
        }
    }

    // ==========================================================
    // ✅ Internal helpers (validation, mapping, pagination)
    // ==========================================================

    private void validateDecisionRequest(Integer ideaId, ReviewerDecisionRequest req) {
        if (ideaId == null) throw new IllegalArgumentException("ideaId is required");
        if (req == null) throw new IllegalArgumentException("Request body is required");
        if (req.getReviewerId() == null) throw new IllegalArgumentException("reviewerId is required");
        if (req.getDecision() == null || req.getDecision().isBlank()) throw new IllegalArgumentException("decision is required");
    }

    private void validateDiscussionRequest(Integer ideaId, Integer userId, Integer stageId, String text) {
        if (ideaId == null) throw new IllegalArgumentException("ideaId is required");
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (stageId == null) throw new IllegalArgumentException("stageId is required");
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text is required");
        if (text.length() > 2000) throw new IllegalArgumentException("text must be <= 2000 characters");
    }

    /**
     * Reviewer decisions: ONLY ACCEPTED/REJECTED/REFINE (NO APPROVED here).
     */
    private Constants.IdeaStatus normalizeReviewerDecision(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("decision is required");
        String v = raw.trim().toUpperCase();

        // Do not allow APPROVED in reviewer module decisions
        if ("APPROVED".equals(v) || "APPROVE".equals(v)) {
            throw new IllegalArgumentException("Use ACCEPTED not APPROVED in reviewer module");
        }

        if ("ACCEPT".equals(v)) v = "ACCEPTED";
        if ("REJECT".equals(v)) v = "REJECTED";

        Constants.IdeaStatus st;
        try {
            st = Constants.IdeaStatus.valueOf(v);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid decision: " + raw + " (use ACCEPTED/REJECTED/REFINE)");
        }

        if (st != Constants.IdeaStatus.ACCEPTED &&
                st != Constants.IdeaStatus.REJECTED &&
                st != Constants.IdeaStatus.REFINE) {
            throw new IllegalArgumentException("Allowed decisions: ACCEPTED / REJECTED / REFINE");
        }
        return st;
    }

    private String normalizeDashboardDecisionFilter(String raw) {
        String v = raw.trim().toUpperCase();
        if ("APPROVE".equals(v) || "APPROVED".equals(v)) v = "ACCEPTED"; // normalize for old UI inputs
        if ("ACCEPT".equals(v)) v = "ACCEPTED";
        if ("REJECT".equals(v)) v = "REJECTED";
        return v;
    }

    private void ensureStageStartMarker(Idea idea, Integer stage, User actor) {
        boolean exists = activityRepo.existsByIdea_IdeaIdAndActivityTypeAndEventAndDeletedFalse(
                idea.getIdeaId(), Constants.ActivityType.CURRENTSTATUS, EVENT_STAGE_START
        );
        if (!exists) {
            // decision can be UNDERREVIEW, event says stage start
            logCurrentStatusActivity(idea, actor, stage, Constants.IdeaStatus.UNDERREVIEW, EVENT_STAGE_START, false, false);
        }
    }

    private Integer findNextStageFromAssignments(Integer ideaId, Integer currentStage) {
        List<AssignedReviewerToIdea> all = reviewerAssignRepo.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(ideaId);
        return all.stream()
                .map(AssignedReviewerToIdea::getStage)
                .filter(Objects::nonNull)
                .filter(s -> s > currentStage)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private UserActivity resolveAndValidateParent(Integer replyParent, Integer ideaId, Integer stageId) {
        if (replyParent == null) return null;

        UserActivity parent = activityRepo.findById(replyParent)
                .orElseThrow(() -> new IllegalArgumentException("Invalid replyParent id: " + replyParent));

        if (parent.isDeleted()) {
            throw new IllegalArgumentException("Parent comment is deleted");
        }
        if (parent.getIdea() == null || !Objects.equals(parent.getIdea().getIdeaId(), ideaId)) {
            throw new IllegalArgumentException("replyParent does not belong to this idea");
        }
        if (!Objects.equals(parent.getStageId(), stageId)) {
            throw new IllegalArgumentException("replyParent does not belong to this stage");
        }
        if (parent.getActivityType() != Constants.ActivityType.REVIEWDISCUSSION) {
            throw new IllegalArgumentException("replyParent is not a reviewer discussion comment");
        }

        // Optional depth guard (block reply-to-reply):
        // if (parent.getReplyParent() != null) throw new IllegalArgumentException("Nested replies not allowed");

        return parent;
    }

    private ReviewerDiscussionDTO toDiscussionDTO(UserActivity activity) {
        return ReviewerDiscussionDTO.builder()
                .userActivityId(activity.getUserActivityId())
                .userId(activity.getUser().getUserId())
                .displayName(activity.getUser().getName())
                .commentText(activity.getCommentText())
                .stageId(activity.getStageId())
                .replyParent(activity.getReplyParent() != null ? activity.getReplyParent().getUserActivityId() : null)
                .createdAt(activity.getCreatedAt())
                .build();
    }

    private ReviewerDashboardDTO toDashboardDTO(AssignedReviewerToIdea a) {
        String displayDecision = (a.getDecision() == null || a.getDecision().isBlank())
                ? "UNDERREVIEW"
                : a.getDecision();

        return ReviewerDashboardDTO.builder()
                .ideaId(a.getIdea().getIdeaId())
                .ideaTitle(a.getIdea().getTitle())
                .employeeName(a.getIdea().getUser() != null ? a.getIdea().getUser().getName() : "Unknown")
                .categoryName(a.getIdea().getCategory() != null ? a.getIdea().getCategory().getName() : "Unknown")
                .assignmentStage(a.getStage())
                .currentIdeaStatus(a.getIdea().getIdeaStatus() != null ? a.getIdea().getIdeaStatus().name() : null)
                .reviewerDecision(displayDecision)
                .assignedDate(a.getCreatedAt())
                .build();
    }

    private <T> PagedResponse<T> buildPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    // ==========================================================
    // ✅ USER ACTIVITY LOGGING HELPERS (ADDED HERE; no other file needed)
    // Timeline:
    // - CURRENTSTATUS: reviewer decisions + status changes + stage events
    // - FINALDECISION: whole idea final outcome only (APPROVED/REJECTED)
    // ==========================================================

    private void logCurrentStatusActivity(
            Idea idea,
            User actor,
            Integer stageId,
            Constants.IdeaStatus decision,
            String event,
            boolean applyXpToActor,
            boolean applyXpToOwner
    ) {
        int delta = gamificationService.getDeltaForIdeaStatus(decision);
        logCurrentStatusActivityWithDelta(idea, actor, stageId, decision, event, delta, applyXpToActor, applyXpToOwner);
    }

    private void logCurrentStatusActivityWithDelta(
            Idea idea,
            User actor,
            Integer stageId,
            Constants.IdeaStatus decision,
            String event,
            int delta,
            boolean applyXpToActor,
            boolean applyXpToOwner
    ) {
        if (idea == null || actor == null || decision == null) return;

        UserActivity ua = UserActivity.builder()
                .idea(idea)
                .user(actor)
                .activityType(Constants.ActivityType.CURRENTSTATUS)
                .decision(decision)
                .event(event)
                .stageId(stageId)
                .delta(delta)
                .commentText(event)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        activityRepo.save(ua);

        if (applyXpToActor && delta != 0) {
            gamificationService.applyDelta(actor.getUserId(), delta);
        }
        if (applyXpToOwner && delta != 0 && idea.getUser() != null) {
            gamificationService.applyDelta(idea.getUser().getUserId(), delta);
        }
    }

    private void logFinalDecisionActivity(
            Idea idea,
            User actor,
            Integer stageId,
            Constants.IdeaStatus finalDecision,
            String event,
            boolean applyXpToActor,
            boolean applyXpToOwner
    ) {
        if (finalDecision != Constants.IdeaStatus.APPROVED && finalDecision != Constants.IdeaStatus.REJECTED) {
            throw new IllegalArgumentException("FINALDECISION must be APPROVED or REJECTED only");
        }
        int delta = gamificationService.getDeltaForActivity(Constants.ActivityType.FINALDECISION);

        UserActivity ua = UserActivity.builder()
                .idea(idea)
                .user(actor)
                .activityType(Constants.ActivityType.FINALDECISION)
                .decision(finalDecision)
                .event(event)
                .stageId(stageId)
                .delta(delta)
                .commentText(event)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        activityRepo.save(ua);

        if (applyXpToActor && delta != 0) {
            gamificationService.applyDelta(actor.getUserId(), delta);
        }
        if (applyXpToOwner && delta != 0 && idea.getUser() != null) {
            gamificationService.applyDelta(idea.getUser().getUserId(), delta);
        }
    }

    // ==========================================================
    // ✅ MATRIX TEST SUPPORT (APPEND-ONLY)
    // Used by controller endpoints:
    //  POST /api/reviewer/idea/{ideaId}/matrix/run?actorUserId=1004
    //  GET  /api/reviewer/idea/{ideaId}/matrix?stageId=1
    // ==========================================================

    public java.util.Map<String, Object> getMatrixSnapshot(Integer ideaId, Integer stageId) {

        if (ideaId == null) throw new IllegalArgumentException("ideaId is required");
        if (stageId == null) throw new IllegalArgumentException("stageId is required");

        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new EntityNotFoundException("Idea not found: " + ideaId));

        List<AssignedReviewerToIdea> stageAssignments =
                reviewerAssignRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(ideaId, stageId);

        long accepted = stageAssignments.stream().filter(a -> "ACCEPTED".equalsIgnoreCase(a.getDecision())).count();
        long rejected = stageAssignments.stream().filter(a -> "REJECTED".equalsIgnoreCase(a.getDecision())).count();
        long refine   = stageAssignments.stream().filter(a -> "REFINE".equalsIgnoreCase(a.getDecision())).count();
        long pending  = stageAssignments.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getDecision())).count();
        long undecided = stageAssignments.stream().filter(a -> a.getDecision() == null || a.getDecision().isBlank()).count();

        boolean refineUsed = activityRepo.existsByIdea_IdeaIdAndActivityTypeAndDecisionAndDeletedFalse(
                ideaId, Constants.ActivityType.CURRENTSTATUS, Constants.IdeaStatus.REFINE
        );

        String suggestedOutcome = computeMatrixOutcomeForSnapshot(accepted, rejected, refine, pending, undecided, refineUsed);

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("ideaId", idea.getIdeaId());
        resp.put("ideaStatus", idea.getIdeaStatus() != null ? idea.getIdeaStatus().name() : null);
        resp.put("currentIdeaStage", idea.getStage());
        resp.put("requestedStage", stageId);

        resp.put("accepted", accepted);
        resp.put("rejected", rejected);
        resp.put("refine", refine);
        resp.put("pending", pending);
        resp.put("undecided", undecided);
        resp.put("refineUsed", refineUsed);

        resp.put("suggestedOutcome", suggestedOutcome);

        return resp;
    }

    /**
     * Force evaluate matrix immediately for testing.
     * - Uses current idea stage.
     * - Applies matrix to idea (may change ideaStatus/stage).
     * - Returns before/after snapshot.
     *
     * IMPORTANT: This will change DB state (for test).
     */
    public java.util.Map<String, Object> forceEvaluateMatrix(Integer ideaId, Integer actorUserId) {

        if (ideaId == null) throw new IllegalArgumentException("ideaId is required");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId is required");

        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new EntityNotFoundException("Idea not found: " + ideaId));

        if (idea.isDeleted()) throw new IllegalArgumentException("Idea is deleted: " + ideaId);

        Integer stage = idea.getStage();
        if (stage == null) throw new IllegalStateException("Idea stage is null");

        User actor = userRepo.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + actorUserId));

        // BEFORE snapshot
        java.util.Map<String, Object> before = getMatrixSnapshot(ideaId, stage);

        // Force resolve now (even if some undecided exist)
        // NOTE: This calls your existing private method in ReviewerService:
        // resolveStageByMatrix(Idea idea, Integer stage, User actor, boolean triggeredBySlaExpiry)
        resolveStageByMatrix(idea, stage, actor, false);

        ideaRepo.save(idea);

        // AFTER snapshot (stage might change if accepted progressed)
        Integer afterStage = idea.getStage() != null ? idea.getStage() : stage;
        java.util.Map<String, Object> after = getMatrixSnapshot(ideaId, afterStage);

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("before", before);
        resp.put("after", after);
        return resp;
    }

    /**
     * Pure compute helper for snapshot (does NOT update DB).
     * Rules:
     * - If refineUsed=true: only ACCEPTED/REJECTED matter; tie => REJECTED.
     * - If refineUsed=false: majority wins, any tie => REFINE.
     * - If no votes at all (only pending/undecided) => WAITING.
     */
    private String computeMatrixOutcomeForSnapshot(
            long accepted, long rejected, long refine, long pending, long undecided, boolean refineUsed) {

        long votes = accepted + rejected + refine;

        if (votes == 0) {
            return "WAITING";
        }

        if (refineUsed) {
            if (accepted > rejected) return Constants.IdeaStatus.ACCEPTED.name();
            if (rejected > accepted) return Constants.IdeaStatus.REJECTED.name();
            return Constants.IdeaStatus.REJECTED.name(); // tie => reject
        }

        // refine not used
        if (accepted > rejected && accepted > refine) return Constants.IdeaStatus.ACCEPTED.name();
        if (rejected > accepted && rejected > refine) return Constants.IdeaStatus.REJECTED.name();
        if (refine > accepted && refine > rejected) return Constants.IdeaStatus.REFINE.name();

        return Constants.IdeaStatus.REFINE.name(); // any tie => refine
    }
}
