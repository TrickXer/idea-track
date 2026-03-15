package com.ideatrack.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.notification.NotificationCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized helper for building and firing system-generated and
 * user-generated notifications across all service layers.
 *
 * Metadata JSON structure (stored in Notification.metadata):
 * {
 *   "redirectTo"  : "/ideas/8",
 *   "triggeredBy" : { "userId": 1, "name": "System" },
 *   "context"     : { "ideaId": 8, "ideaTitle": "...", "stage": 2, "action": "IDEA_APPROVED" }
 * }
 *
 * The frontend uses:
 *   - redirectTo  → navigate when notification is clicked
 *   - triggeredBy → show who triggered it
 *   - context     → icon selection, rich message rendering
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHelper {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    // 1.  IDEA MODULE
    // ─────────────────────────────────────────────────────────────

    /**
     * Notify the idea owner that their idea was submitted (system-generated).
     */
    public void notifyIdeaSubmitted(Idea idea, User actor) {
        String metadata = buildMetadata(
                "/my-idea/" + idea.getIdeaId(),
                actor,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "stage",     idea.getStage(),
                        "action",    "IDEA_SUBMITTED"
                )
        );
        send(
                idea.getUser().getUserId(),
                "IDEA_SUBMISSION",
                "Idea Submitted",
                "Your idea \"" + idea.getTitle() + "\" has been submitted for review.",
                "MEDIUM",
                metadata
        );
    }

    /**
     * Notify the idea owner that their idea has been accepted (final stage).
     */
    public void notifyIdeaAccepted(Idea idea, User reviewer) {
        String metadata = buildMetadata(
                "/my-idea/" + idea.getIdeaId(),
                reviewer,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "stage",     idea.getStage(),
                        "action",    "IDEA_APPROVED"
                )
        );
        send(
                idea.getUser().getUserId(),
                "APPROVAL_STATUS",
                "Idea Accepted 🎉",
                "Your idea \"" + idea.getTitle() + "\" has been accepted!",
                "HIGH",
                metadata
        );
    }

    /**
     * Notify the idea owner that their idea has been rejected.
     */
    public void notifyIdeaRejected(Idea idea, User reviewer) {
        String metadata = buildMetadata(
                "/my-idea/" + idea.getIdeaId(),
                reviewer,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "stage",     idea.getStage(),
                        "action",    "IDEA_REJECTED"
                )
        );
        send(
                idea.getUser().getUserId(),
                "REJECTION_NOTICE",
                "Idea Rejected",
                "Your idea \"" + idea.getTitle() + "\" was rejected. Check the feedback.",
                "HIGH",
                metadata
        );
    }

    /**
     * Notify the idea owner that their idea needs refinement.
     */
    public void notifyIdeaRefine(Idea idea, User reviewer) {
        String metadata = buildMetadata(
                "/my-idea/" + idea.getIdeaId(),
                reviewer,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "stage",     idea.getStage(),
                        "action",    "IDEA_REFINE"
                )
        );
        send(
                idea.getUser().getUserId(),
                "FEEDBACK_RECEIVED",
                "Idea Needs Refinement",
                "Your idea \"" + idea.getTitle() + "\" has been sent back for refinement.",
                "MEDIUM",
                metadata
        );
    }

    /**
     * Notify the idea owner that their idea advanced to a new stage.
     */
    public void notifyIdeaStageAdvanced(Idea idea, User reviewer, int newStage) {
        String metadata = buildMetadata(
                "/my-idea/" + idea.getIdeaId(),
                reviewer,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "stage",     newStage,
                        "action",    "IDEA_STAGE_ADVANCED"
                )
        );
        send(
                idea.getUser().getUserId(),
                "APPROVAL_STATUS",
                "Idea Advanced to Stage " + newStage,
                "Your idea \"" + idea.getTitle() + "\" moved to stage " + newStage + " of review.",
                "MEDIUM",
                metadata
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 2.  COMMENT MODULE (UserActivityService)
    // ─────────────────────────────────────────────────────────────

    /**
     * Notify the idea owner that someone commented on their idea.
     * The commenter does NOT get notified about their own comment.
     */
    public void notifyCommentAdded(Idea idea, User commenter) {
        // Don't notify owner if they comment on their own idea
        if (idea.getUser().getUserId().equals(commenter.getUserId())) return;

        String metadata = buildMetadata(
                "/idea/" + idea.getIdeaId(),
                commenter,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "action",    "COMMENT_ADDED"
                )
        );
        send(
                idea.getUser().getUserId(),
                "COMMENT_MENTION",
                "New Comment on Your Idea",
                commenter.getName() + " commented on your idea \"" + idea.getTitle() + "\".",
                "LOW",
                metadata
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 3.  VOTE MODULE (UserActivityService)
    // ─────────────────────────────────────────────────────────────

    /**
     * Notify the idea owner that someone voted on their idea.
     */
    public void notifyVoteCast(Idea idea, User voter, String voteType) {
        if (idea.getUser().getUserId().equals(voter.getUserId())) return;

        String metadata = buildMetadata(
                "/idea/" + idea.getIdeaId(),
                voter,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "voteType",  voteType,
                        "action",    "VOTE_CAST"
                )
        );
        send(
                idea.getUser().getUserId(),
                "IDEA_SUBMISSION",
                "Someone Voted on Your Idea",
                voter.getName() + " cast a " + voteType.toLowerCase() + " on \"" + idea.getTitle() + "\".",
                "LOW",
                metadata
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 4.  REVIEWER MODULE – assigned to an idea
    // ─────────────────────────────────────────────────────────────

    /**
     * Notify a reviewer that they have been assigned to review an idea.
     */
    public void notifyReviewerAssigned(Idea idea, User reviewer, int stage) {
        String metadata = buildMetadata(
                "/reviewer/ideas/" + idea.getIdeaId(),
                systemUser(),
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "stage",     stage,
                        "action",    "REVIEWER_ASSIGNED"
                )
        );
        send(
                reviewer.getUserId(),
                "REVIEW_ASSIGNMENT",
                "New Idea Assigned for Review",
                "You have been assigned to review \"" + idea.getTitle() + "\" at stage " + stage + ".",
                "HIGH",
                metadata
        );
    }

    /**
     * Notify the idea owner that a reviewer posted feedback on their idea.
     */
    public void notifyFeedbackPosted(Idea idea, User reviewer) {
        String metadata = buildMetadata(
                "/my-idea/" + idea.getIdeaId(),
                reviewer,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "stage",     idea.getStage(),
                        "action",    "FEEDBACK_POSTED"
                )
        );
        send(
                idea.getUser().getUserId(),
                "FEEDBACK_RECEIVED",
                "Feedback Received on Your Idea",
                reviewer.getName() + " posted feedback on your idea \"" + idea.getTitle() + "\".",
                "MEDIUM",
                metadata
        );
    }

    /**
     * Notify the idea owner that a reviewer posted a discussion comment.
     */
    public void notifyReviewerDiscussion(Idea idea, User reviewer) {
        if (idea.getUser().getUserId().equals(reviewer.getUserId())) return;
        String metadata = buildMetadata(
                "/my-idea/" + idea.getIdeaId(),
                reviewer,
                Map.of(
                        "ideaId",    idea.getIdeaId(),
                        "ideaTitle", idea.getTitle(),
                        "stage",     idea.getStage(),
                        "action",    "REVIEWER_DISCUSSION"
                )
        );
        send(
                idea.getUser().getUserId(),
                "FEEDBACK_RECEIVED",
                "Reviewer Discussion Update",
                reviewer.getName() + " posted a discussion note on your idea \"" + idea.getTitle() + "\".",
                "LOW",
                metadata
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 5.  PROPOSAL MODULE
    // ─────────────────────────────────────────────────────────────

    /**
     * Notify the proposal owner that their proposal was submitted for review.
     */
    public void notifyProposalSubmitted(Proposal proposal, User actor) {
        String metadata = buildMetadata(
                "/employee/accepted-ideas",
                actor,
                Map.of(
                        "ideaId",    proposal.getIdea() != null ? proposal.getIdea().getIdeaId() : 0,
                        "ideaTitle", proposal.getIdea() != null ? proposal.getIdea().getTitle() : "Unknown",
                        "proposalId", proposal.getProposalId(),
                        "action",    "PROPOSAL_SUBMITTED"
                )
        );
        send(
                proposal.getUser().getUserId(),
                "IDEA_SUBMISSION",
                "Proposal Submitted",
                "Your proposal for \"" + (proposal.getIdea() != null ? proposal.getIdea().getTitle() : "idea") + "\" has been submitted for review.",
                "MEDIUM",
                metadata
        );
    }

    /**
     * Notify the proposal owner that their proposal was approved.
     */
    public void notifyProposalApproved(Proposal proposal, User reviewer) {
        String metadata = buildMetadata(
                "/employee/accepted-ideas",
                reviewer,
                Map.of(
                        "ideaId",    proposal.getIdea() != null ? proposal.getIdea().getIdeaId() : 0,
                        "ideaTitle", proposal.getIdea() != null ? proposal.getIdea().getTitle() : "Unknown",
                        "proposalId", proposal.getProposalId(),
                        "action",    "PROPOSAL_APPROVED"
                )
        );
        send(
                proposal.getUser().getUserId(),
                "APPROVAL_STATUS",
                "Proposal Approved 🎉",
                "Your proposal for \"" + (proposal.getIdea() != null ? proposal.getIdea().getTitle() : "idea") + "\" has been approved!",
                "HIGH",
                metadata
        );
    }

    /**
     * Notify the proposal owner that their proposal was rejected.
     */
    public void notifyProposalRejected(Proposal proposal, User reviewer) {
        String metadata = buildMetadata(
                "/employee/accepted-ideas",
                reviewer,
                Map.of(
                        "ideaId",    proposal.getIdea() != null ? proposal.getIdea().getIdeaId() : 0,
                        "ideaTitle", proposal.getIdea() != null ? proposal.getIdea().getTitle() : "Unknown",
                        "proposalId", proposal.getProposalId(),
                        "action",    "PROPOSAL_REJECTED"
                )
        );
        send(
                proposal.getUser().getUserId(),
                "REJECTION_NOTICE",
                "Proposal Rejected",
                "Your proposal for \"" + (proposal.getIdea() != null ? proposal.getIdea().getTitle() : "idea") + "\" was rejected.",
                "HIGH",
                metadata
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 6.  GAMIFICATION MODULE – badges & level-up
    // ─────────────────────────────────────────────────────────────

    /**
     * System notification: user earned a new badge.
     */
    public void notifyBadgeEarned(User user, String badgeName) {
        String metadata = buildMetadata(
                "/profile",
                systemUser(),
                Map.of(
                        "badgeName", badgeName,
                        "action",    "BADGE_EARNED"
                )
        );
        send(
                user.getUserId(),
                "ACHIEVEMENT_UNLOCKED",
                "New Badge Earned 🏅",
                "You earned the badge: \"" + badgeName + "\"!",
                "MEDIUM",
                metadata
        );
    }

    /**
     * System notification: user reached a new XP level/tier.
     */
    public void notifyLevelUp(User user, String newLevel) {
        String metadata = buildMetadata(
                "/profile",
                systemUser(),
                Map.of(
                        "level",  newLevel,
                        "action", "LEVEL_UP"
                )
        );
        send(
                user.getUserId(),
                "ACHIEVEMENT_UNLOCKED",
                "Level Up! 🚀",
                "Congratulations! You reached level: " + newLevel + ".",
                "HIGH",
                metadata
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 7.  REVIEWER ASSIGNMENT MODULE
    // ─────────────────────────────────────────────────────────────

    /**
     * Notify an admin/super-admin that a reviewer's SLA has expired for an idea.
     */
    public void notifyReviewerSlaExpired(Idea idea, User reviewer) {
        String metadata = buildMetadata(
                "/admin",
                systemUser(),
                Map.of(
                        "ideaId",       idea.getIdeaId(),
                        "ideaTitle",    idea.getTitle(),
                        "reviewerName", reviewer.getName(),
                        "stage",        idea.getStage(),
                        "action",       "REVIEWER_SLA_EXPIRED"
                )
        );
        send(
                reviewer.getUserId(),
                "DEADLINE_REMINDER",
                "Review SLA Expired",
                "Your review window for \"" + idea.getTitle() + "\" stage " + idea.getStage() + " has expired.",
                "HIGH",
                metadata
        );
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private void send(Integer toUserId, String type, String title, String message, String priority, String metadata) {
        try {
            NotificationCreateRequest req = NotificationCreateRequest.builder()
                    .userId(toUserId)
                    .notificationType(type)
                    .notificationTitle(title)
                    .notificationMessage(message)
                    .priority(priority)
                    .metadata(metadata)
                    .build();
            notificationService.create(req);
        } catch (Exception ex) {
            // Notifications must never break the main business flow
            log.warn("Failed to send notification to userId={}: {}", toUserId, ex.getMessage());
        }
    }

    /**
     * Builds the metadata JSON string.
     * Structure: { "redirectTo": "...", "triggeredBy": { "userId": X, "name": "Y" }, "context": { ... } }
     */
    private String buildMetadata(String redirectTo, Map<String, Object> triggeredBy, Map<String, Object> context) {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("redirectTo", redirectTo);
            root.put("triggeredBy", triggeredBy);
            root.put("context", context);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("Failed to build notification metadata: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> triggeredBy(User actor) {
        Map<String, Object> by = new LinkedHashMap<>();
        by.put("userId", actor.getUserId());
        by.put("name", actor.getName());
        return by;
    }

    /**
     * Convenience: build metadata with a User object as triggeredBy.
     */
    String buildMetadata(String redirectTo, User actor, Map<String, Object> context) {
        return buildMetadata(redirectTo, triggeredBy(actor), context);
    }

    /**
     * A virtual "System" triggeredBy — used for automated / scheduler-fired notifications.
     */
    private Map<String, Object> systemUser() {
        Map<String, Object> sys = new LinkedHashMap<>();
        sys.put("userId", 0);
        sys.put("name", "System");
        return sys;
    }
}
