package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants.IdeaStatus;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.reviewer.ProposalDecisionRequest;
import com.ideatrack.main.repository.IProposalRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.service.NotificationHelper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//Done By Vibhuti  //tested 100%//

@ExtendWith(MockitoExtension.class)
public class TestProposalReviewService {

    @Mock
    private IProposalRepository proposalRepository;

    @Mock
    private IUserActivityRepository activityRepo;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private NotificationHelper notificationHelper;

    @InjectMocks
    private ProposalReviewService service;

    // ---------------- Helpers ----------------

    private Proposal proposalWith(IdeaStatus status) {
        Proposal p = new Proposal();
        p.setIdeaStatus(status);
        return p;
    }

    private ProposalDecisionRequest decisionReq(String decision) {
        ProposalDecisionRequest r = new ProposalDecisionRequest();
        r.setDecision(decision);
        return r;
    }

    // ---------------- initializeProposal ----------------

    @Test
    void initializeProposal_shouldSetProjectProposal_whenIdeaAccepted() {
        int proposalId = 100;
        Proposal proposal = proposalWith(IdeaStatus.ACCEPTED);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        service.initializeProposal(proposalId);

        assertEquals(IdeaStatus.PROJECTPROPOSAL, proposal.getIdeaStatus());
        verify(proposalRepository).save(proposal);
    }

    @Test
    void initializeProposal_shouldThrow_whenIdeaNotAccepted() {
        int proposalId = 101;
        Proposal proposal = proposalWith(IdeaStatus.UNDERREVIEW);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.initializeProposal(proposalId));
        assertTrue(ex.getMessage().contains("Only ACCEPTED ideas/proposals can move to PROJECTPROPOSAL"));

        verify(proposalRepository, never()).save(any());
    }

    @Test
    void initializeProposal_shouldThrow_whenProposalNotFound() {
        when(proposalRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.initializeProposal(999));
        verify(proposalRepository, never()).save(any());
    }

    // ---------------- startProposalReview ----------------

    @Test
    void startProposalReview_shouldMoveToUnderReview_whenInProjectProposal() {
        int proposalId = 200;
        Proposal proposal = proposalWith(IdeaStatus.PROJECTPROPOSAL);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        service.startProposalReview(proposalId);

        assertEquals(IdeaStatus.UNDERREVIEW, proposal.getIdeaStatus());
        verify(proposalRepository).save(proposal);
    }

    @Test
    void startProposalReview_shouldThrow_whenNotInProjectProposal() {
        int proposalId = 201;
        Proposal proposal = proposalWith(IdeaStatus.ACCEPTED); // NOT in PROJECTPROPOSAL
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        // IMPORTANT: startProposalReview uses 'require' -> IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.startProposalReview(proposalId));
        assertTrue(ex.getMessage().contains("Start review only from PROJECTPROPOSAL"));

        verify(proposalRepository, never()).save(any());
    }

    @Test
    void startProposalReview_shouldThrow_whenProposalNotFound() {
        when(proposalRepository.findById(998)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.startProposalReview(998));
        verify(proposalRepository, never()).save(any());
    }

    // ---------------- processDecision ----------------

    @Test
    void processDecision_shouldThrow_whenStillInProjectProposal() {
        int proposalId = 300;
        Proposal proposal = proposalWith(IdeaStatus.PROJECTPROPOSAL);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.processDecision(proposalId, decisionReq("APPROVED")));

        // For Spring 6 / Boot 3:
        try {
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        } catch (NoSuchMethodError | Exception ignored) {
            // For Spring 5 / Boot 2:
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
        assertTrue(ex.getReason().contains("reviewer not assigned"));

        verify(proposalRepository, never()).save(any());
    }

    @Test
    void processDecision_shouldThrow_whenNotUnderReview() {
        int proposalId = 301;
        Proposal proposal = proposalWith(IdeaStatus.ACCEPTED);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.processDecision(proposalId, decisionReq("APPROVED")));

        try {
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        } catch (NoSuchMethodError | Exception ignored) {
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
        assertTrue(ex.getReason().contains("Decision allowed only when proposal is UNDERREVIEW"));

        verify(proposalRepository, never()).save(any());
    }

    @Test
    void processDecision_shouldApprove_whenDecisionApproved_caseInsensitive() {
        int proposalId = 302;
        Proposal proposal = proposalWith(IdeaStatus.UNDERREVIEW);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        ProposalDecisionRequest req = decisionReq("approved"); // mixed case

        service.processDecision(proposalId, req);

        assertEquals(IdeaStatus.APPROVED, proposal.getIdeaStatus());
        verify(proposalRepository).save(proposal);
    }

    @Test
    void processDecision_shouldReject_whenDecisionRejected() {
        int proposalId = 303;
        Proposal proposal = proposalWith(IdeaStatus.UNDERREVIEW);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        ProposalDecisionRequest req = decisionReq("REJECTED");

        service.processDecision(proposalId, req);

        assertEquals(IdeaStatus.REJECTED, proposal.getIdeaStatus());
        verify(proposalRepository).save(proposal);
    }

    @Test
    void processDecision_shouldThrow_whenProposalNotFound() {
        when(proposalRepository.findById(997)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.processDecision(997, decisionReq("APPROVED")));

        verify(proposalRepository, never()).save(any());
    }

    // ------------- Optional: verify saved instance via captor -------------

    @Test
    void initializeProposal_shouldSaveUpdatedEntity() {
        int proposalId = 400;
        Proposal proposal = proposalWith(IdeaStatus.ACCEPTED);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        service.initializeProposal(proposalId);

        ArgumentCaptor<Proposal> cap = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(cap.capture());
        Proposal saved = cap.getValue();
        assertEquals(IdeaStatus.PROJECTPROPOSAL, saved.getIdeaStatus());
    }
}