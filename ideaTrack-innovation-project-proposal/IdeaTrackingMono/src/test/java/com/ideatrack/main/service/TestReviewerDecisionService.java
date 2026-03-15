package com.ideatrack.main.service;

import com.ideatrack.main.data.AssignedReviewerToIdea;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.reviewer.ReviewerDecisionRequest;
import com.ideatrack.main.exception.ReviewerBadRequestException;
import com.ideatrack.main.exception.ReviewerForbiddenException;
import com.ideatrack.main.exception.ReviewerNotFoundException;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IReviewerCategoryRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestReviewerDecisionService {

    @Mock private IIdeaRepository ideaRepo;
    @Mock private IAssignedReviewerToIdeaRepository reviewerAssignRepo;
    @Mock private IUserActivityRepository activityRepo;
    @Mock private IReviewerCategoryRepository reviewerCategoryRepo;
    @Mock private IUserRepository userRepo;
    @Mock private GamificationService gamificationService;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ReviewerDecisionService decisionService;

    private Idea testIdea;
    private User testReviewer;
    private ReviewerDecisionRequest validRequest;

    @BeforeEach
    void setUp() {
        testIdea = new Idea();
        testIdea.setIdeaId(1);
        testIdea.setStage(1);
        testIdea.setDeleted(false);

        testReviewer = new User();
        testReviewer.setUserId(10);
        testReviewer.setRole(Constants.Role.REVIEWER);
        testReviewer.setEmail("reviewer@test.com");

        validRequest = new ReviewerDecisionRequest();
        validRequest.setDecision("ACCEPTED");
        validRequest.setFeedback("Great innovation.");

        // Setup Security Context Mocking
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should throw exception if ideaId is missing")
    void processDecision_IdeaIdNull() {
        assertThatThrownBy(() -> decisionService.processDecision(null, validRequest))
                .isInstanceOf(ReviewerBadRequestException.class)
                .hasMessageContaining("ideaId is required");
    }

    @Test
    @DisplayName("Should throw exception if decision is missing in request")
    void processDecision_DecisionNull() {
        validRequest.setDecision(null);
        assertThatThrownBy(() -> decisionService.processDecision(1, validRequest))
                .isInstanceOf(ReviewerBadRequestException.class); 
    }

    @Test
    @DisplayName("Should throw exception if non-reviewer tries to submit decision")
    void processDecision_NotAReviewer() {
        // Setup Auth to return a valid email
        setupMockAuth();
        
        // Mock the user as an EMPLOYEE instead of a REVIEWER 
        testReviewer.setRole(Constants.Role.EMPLOYEE); 
        
        // Mock the lookups required before the role check
        when(ideaRepo.findById(1)).thenReturn(Optional.of(testIdea));
        when(userRepo.findById(anyInt())).thenReturn(Optional.of(testReviewer));

        // The service should catch the role mismatch here 
        assertThatThrownBy(() -> decisionService.processDecision(1, validRequest))
                .isInstanceOf(ReviewerForbiddenException.class)
                .hasMessageContaining("Only REVIEWER can submit decisions");
    }

    @Test
    @DisplayName("Should throw exception if reviewer uses 'APPROVED' instead of 'ACCEPTED'")
    void processDecision_RejectApprovedKeyword() {
        // Set the forbidden keyword in the request
        validRequest.setDecision("APPROVED");
        
        setupMockAuth();
        when(ideaRepo.findById(1)).thenReturn(Optional.of(testIdea));
        when(userRepo.findById(10)).thenReturn(Optional.of(testReviewer));

        // To reach the keyword check, we MUST provide a mock assignment 
        // Otherwise, it fails with "Reviewer not assigned to current stage"
        AssignedReviewerToIdea assignment = new AssignedReviewerToIdea();
        when(reviewerAssignRepo.findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(1, 10, 1))
                .thenReturn(Optional.of(assignment));

        // Now execution reaches normalizeReviewerDecision [cite: 22, 48]
        assertThatThrownBy(() -> decisionService.processDecision(1, validRequest))
                .isInstanceOf(ReviewerBadRequestException.class)
                .hasMessageContaining("Use ACCEPTED not APPROVED in reviewer decisions");
    }

    @Test
    @DisplayName("Should block decision if reviewer is not assigned to current stage")
    void processDecision_NotAssigned() {
        setupMockAuth();
        when(ideaRepo.findById(1)).thenReturn(Optional.of(testIdea));
        when(userRepo.findById(10)).thenReturn(Optional.of(testReviewer));
        
        // Return empty for assignment check
        when(reviewerAssignRepo.findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(1, 10, 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> decisionService.processDecision(1, validRequest))
                .isInstanceOf(ReviewerForbiddenException.class)
                .hasMessageContaining("Reviewer not assigned to current stage"); 
    }

    @Test
    @DisplayName("Should prevent changing a decision once it is locked")
    void processDecision_LockedDecision() {
        setupMockAuth();
        when(ideaRepo.findById(1)).thenReturn(Optional.of(testIdea));
        when(userRepo.findById(10)).thenReturn(Optional.of(testReviewer));

        AssignedReviewerToIdea assignment = new AssignedReviewerToIdea();
        assignment.setDecision("REJECTED"); // Existing decision [cite: 23]

        when(reviewerAssignRepo.findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(1, 10, 1))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> decisionService.processDecision(1, validRequest))
                .isInstanceOf(ReviewerForbiddenException.class)
                .hasMessageContaining("Decision already submitted for this stage");
    }

    /**
     * Helper to mock Spring Security context
     */
    private void setupMockAuth() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("reviewer@test.com");
        when(userRepo.findByEmail("reviewer@test.com")).thenReturn(Optional.of(testReviewer));
    }
}