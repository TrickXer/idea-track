/**
 * Author - Pavan
 */
package com.ideatrack.main.service;

import java.time.LocalDateTime;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.data.User;
import com.ideatrack.main.data.UserActivity;
import com.ideatrack.main.repository.IUserActivityRepository;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReviewerTimelineUtil {

    public static void logCurrentStatus(IUserActivityRepository activityRepo,
                                        GamificationService gamificationService,
                                        Idea idea,
                                        User actor,
                                        Integer stageId,
                                        Constants.IdeaStatus decision,
                                        String event,
                                        boolean applyXpToActor,
                                        boolean applyXpToOwner) {

        if (idea == null || actor == null || decision == null) {
            return;
        }
        if (decision == Constants.IdeaStatus.APPROVED) {
            decision = Constants.IdeaStatus.ACCEPTED;
        }

        int delta = gamificationService.getDeltaForIdeaStatus(decision);

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

    public static void logCurrentStatusWithDelta(IUserActivityRepository activityRepo,
                                                 GamificationService gamificationService,
                                                 Idea idea,
                                                 User actor,
                                                 Integer stageId,
                                                 Constants.IdeaStatus decision,
                                                 String event,
                                                 int deltaOverride,
                                                 boolean applyXpToActor,
                                                 boolean applyXpToOwner) {

        if (idea == null || actor == null || decision == null) {
            return;
        }

        UserActivity ua = UserActivity.builder()
                .idea(idea)
                .user(actor)
                .activityType(Constants.ActivityType.CURRENTSTATUS)
                .decision(decision)
                .event(event)
                .stageId(stageId)
                .delta(deltaOverride)
                .commentText(event)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        activityRepo.save(ua);

        if (applyXpToActor && deltaOverride != 0) {
            gamificationService.applyDelta(actor.getUserId(), deltaOverride);
        }
        if (applyXpToOwner && deltaOverride != 0 && idea.getUser() != null) {
            gamificationService.applyDelta(idea.getUser().getUserId(), deltaOverride);
        }
    }

    public static void logFinalDecision(IUserActivityRepository activityRepo,
                                        GamificationService gamificationService,
                                        Idea idea,
                                        User actor,
                                        Integer stageId,
                                        Constants.IdeaStatus finalDecision,
                                        String event,
                                        boolean applyXpToActor,
                                        boolean applyXpToOwner) {

        if (finalDecision != Constants.IdeaStatus.ACCEPTED
                && finalDecision != Constants.IdeaStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "FINALDECISION must be ACCEPTED or REJECTED only"
            );
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


    /**
     * Logs proposal events under FINALDECISION with any proposal decision:
     * DRAFT / PROJECTPROPOSAL / APPROVED / REJECTED
     * Event examples: PROPOSAL_CREATED / PROPOSAL_SUBMITTED / PROPOSAL_APPROVED / PROPOSAL_REJECTED
     */
    public static void logProposalFinalDecision(IUserActivityRepository activityRepo,
                                                GamificationService gamificationService,
                                                Proposal proposal,
                                                User actor,
                                                Constants.IdeaStatus decision,
                                                String event,
                                                boolean applyXpToActor,
                                                boolean applyXpToOwner) {
        if (proposal == null || proposal.getIdea() == null || actor == null || decision == null) {
            return;
        }

        // For proposals we keep APPROVED as-is (no conversion to ACCEPTED).
        int delta = gamificationService.getDeltaForIdeaStatus(decision);

        User ideaOwner = proposal.getIdea().getUser();
        User proposalOwner = proposal.getUser();
        Integer ownerUserIdForXp = (proposalOwner != null ? proposalOwner.getUserId()
                                                          : (ideaOwner != null ? ideaOwner.getUserId() : null));

        UserActivity ua = UserActivity.builder()
                .idea(proposal.getIdea())
                .user(actor)
                .activityType(Constants.ActivityType.CURRENTSTATUS)
                .decision(decision)
                .event(event)
                .stageId(null)
                .delta(delta)
                .commentText(event)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        activityRepo.save(ua);

        if (applyXpToActor && delta != 0) {
            gamificationService.applyDelta(actor.getUserId(), delta);
        }
        if (applyXpToOwner && delta != 0 && ownerUserIdForXp != null) {
            gamificationService.applyDelta(ownerUserIdForXp, delta);
        }
    }

    public static void logProposalFinalDecisionWithDelta(IUserActivityRepository activityRepo,
                                                         GamificationService gamificationService,
                                                         Proposal proposal,
                                                         User actor,
                                                         Constants.IdeaStatus decision,
                                                         String event,
                                                         int deltaOverride,
                                                         boolean applyXpToActor,
                                                         boolean applyXpToOwner) {
        if (proposal == null || proposal.getIdea() == null || actor == null || decision == null) {
            return;
        }

        User ideaOwner = proposal.getIdea().getUser();
        User proposalOwner = proposal.getUser();
        Integer ownerUserIdForXp = (proposalOwner != null ? proposalOwner.getUserId()
                                                          : (ideaOwner != null ? ideaOwner.getUserId() : null));

        UserActivity ua = UserActivity.builder()
                .idea(proposal.getIdea())
                .user(actor)
                .activityType(Constants.ActivityType.FINALDECISION)
                .decision(decision)
                .event(event)
                .stageId(null)
                .delta(deltaOverride)
                .commentText(event)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        activityRepo.save(ua);

        if (applyXpToActor && deltaOverride != 0) {
            gamificationService.applyDelta(actor.getUserId(), deltaOverride);
        }
        if (applyXpToOwner && deltaOverride != 0 && ownerUserIdForXp != null) {
            gamificationService.applyDelta(ownerUserIdForXp, deltaOverride);
        }
    }
}