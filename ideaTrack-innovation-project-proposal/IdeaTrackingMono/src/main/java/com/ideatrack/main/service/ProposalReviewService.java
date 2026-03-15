package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants.IdeaStatus;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.dto.reviewer.ProposalDecisionRequest;
import com.ideatrack.main.repository.IProposalRepository;
import com.ideatrack.main.repository.IUserActivityRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Objects;

// Done by Vibhuti

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalReviewService {

    // --- Event labels (consistent with your proposal logging convention) ---
    private static final String EVENT_PROPOSAL_APPROVED  = "PROPOSAL_APPROVED";
    private static final String EVENT_PROPOSAL_REJECTED  = "PROPOSAL_REJECTED";
    // (We keep CREATED/SUBMITTED in ProposalService to avoid duplicate logs)

    private final IProposalRepository proposalRepository;

    // ✅ Inject activity + gamification to log like ReviewerDecisionService
    private final IUserActivityRepository activityRepo;
    private final GamificationService gamificationService;
    private final NotificationHelper notificationHelper;

    // --- Proposal lifecycle ---

    @Transactional
    public void initializeProposal(Integer proposalId) {
        Proposal proposal = find(proposalId);
        require(proposal.getIdeaStatus() == IdeaStatus.ACCEPTED,
                "Only ACCEPTED ideas/proposals can move to PROJECTPROPOSAL");

        // Move to 'PROJECTPROPOSAL'
        proposal.setIdeaStatus(IdeaStatus.PROJECTPROPOSAL);
        setReviewContextSafe(proposal, "PROPOSAL");
        setProposalStatusSafe(proposal, "PROJECTPROPOSAL");
        proposalRepository.save(proposal);

        // NOTE:
        // We are not logging here to avoid duplicating "PROPOSAL_SUBMITTED" already logged
        // in ProposalService.submit(...). If THIS method is your only submission path,
        // you can optionally log there using:
        // ReviewerTimelineUtil.logProposalFinalDecision(activityRepo, gamificationService,
        //     proposal, proposal.getUser(), IdeaStatus.PROJECTPROPOSAL, "PROPOSAL_SUBMITTED", true, false);
    }

    @Transactional
    public void startProposalReview(Integer proposalId) {
        Proposal proposal = find(proposalId);
        require(proposal.getIdeaStatus() == IdeaStatus.PROJECTPROPOSAL,
                "Start review only from PROJECTPROPOSAL");

        proposal.setIdeaStatus(IdeaStatus.UNDERREVIEW);
        setReviewContextSafe(proposal, "PROPOSAL");
        setProposalStatusSafe(proposal, "UNDERREVIEW");
        proposalRepository.save(proposal);

        // NOTE:
        // You decided earlier to keep only 4 events (CREATED, SUBMITTED, APPROVED, REJECTED).
        // Hence, we are not logging UNDERREVIEW here. If you want it later:
        // ReviewerTimelineUtil.logProposalFinalDecision(activityRepo, gamificationService,
        //     proposal, proposal.getUser(), IdeaStatus.UNDERREVIEW, "PROPOSAL_REVIEW_START", true, false);
    }

    @Transactional
    public void processDecision(Integer proposalId, ProposalDecisionRequest request) {
        Proposal proposal = find(proposalId);

        // 1) Guard: If still in PROJECTPROPOSAL, reviewer not assigned yet
        if (proposal.getIdeaStatus() == IdeaStatus.PROJECTPROPOSAL) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Proposal is submitted, reviewer not assigned"
            );
        }

        // 2) Only allow decisions in UNDERREVIEW
        if (proposal.getIdeaStatus() != IdeaStatus.UNDERREVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Decision allowed only when proposal is UNDERREVIEW"
            );
        }

        // 3) Parse decision safely
        String d = Objects.toString(request.getDecision(), "")
                          .trim()
                          .toUpperCase(Locale.ROOT);

        switch (d) {
            case "APPROVED":
                proposal.setIdeaStatus(IdeaStatus.APPROVED);
                proposalRepository.save(proposal);

                // ✅ FINALDECISION log: APPROVED
                ReviewerTimelineUtil.logProposalFinalDecision(
                        activityRepo,
                        gamificationService,
                        proposal,
                        // Actor: If you later pass reviewer user, use that here.
                        proposal.getUser(),
                        IdeaStatus.APPROVED,
                        EVENT_PROPOSAL_APPROVED,
                        true,   // apply XP to actor
                        false   // do not double-apply to owner
                );
                notificationHelper.notifyProposalApproved(proposal, proposal.getUser());
                break;

            case "REJECTED":
                proposal.setIdeaStatus(IdeaStatus.REJECTED);
                proposalRepository.save(proposal);

                // ✅ FINALDECISION log: REJECTED
                ReviewerTimelineUtil.logProposalFinalDecision(
                        activityRepo,
                        gamificationService,
                        proposal,
                        // Actor: If you later pass reviewer user, use that here.
                        proposal.getUser(),
                        IdeaStatus.REJECTED,
                        EVENT_PROPOSAL_REJECTED,
                        true,   // apply XP to actor
                        false   // do not double-apply to owner
                );
                notificationHelper.notifyProposalRejected(proposal, proposal.getUser());
                break;

            default:
                // Keep this strict so clients know what's supported
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Allowed decision values: APPROVED or REJECTED"
                );
        }
    }

    private Proposal find(Integer proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + proposalId));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private void setReviewContextSafe(Proposal proposal, String ctx) {
        try {
            proposal.getClass().getMethod("setReviewContext", String.class).invoke(proposal, ctx);
        } catch (Exception e) {
            log.warn("Failed to set review context via reflection: {}", e.getMessage());
        }
    }

    private void setProposalStatusSafe(Proposal proposal, String status) {
        try {
            proposal.getClass().getMethod("setProposalStatus", String.class).invoke(proposal, status);
        } catch (Exception e) {
            log.warn("Failed to set proposal status via reflection: {}", e.getMessage());
        }
    }
}