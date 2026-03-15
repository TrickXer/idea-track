package com.ideatrack.main.service;

import com.ideatrack.main.data.AssignedReviewerToIdea;
import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IReviewerCategoryRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;
import com.ideatrack.main.service.NotificationHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestReviewerAssignmentService {

    @Mock
    private IIdeaRepository ideaRepo;

    @Mock
    private IAssignedReviewerToIdeaRepository reviewerAssignRepo;

    @Mock
    private IUserActivityRepository activityRepo;

    @Mock
    private IReviewerCategoryRepository reviewerCategoryRepo;

    @Mock
    private IUserRepository userRepo;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private NotificationHelper notificationHelper;

    @InjectMocks
    private ReviewerAssignmentService reviewerAssignmentService;

    private Idea testIdea;
    private Category testCategory;
    private User testOwner;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setCategoryId(101);

        testOwner = new User();
        testOwner.setUserId(1);
        testOwner.setEmail("owner@example.com");

        testIdea = new Idea();
        testIdea.setIdeaId(500);
        testIdea.setIdeaStatus(Constants.IdeaStatus.SUBMITTED);
        testIdea.setCategory(testCategory);
        testIdea.setUser(testOwner);
        testIdea.setDeleted(false);
    }

    @Test
    @DisplayName("Should skip processing when no submitted ideas are found")
    void assignSubmittedIdeas_NoIdeasFound() {
        when(ideaRepo.findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus.SUBMITTED))
                .thenReturn(Collections.emptyList());

        reviewerAssignmentService.assignSubmittedIdeasEndOfDay();

        verify(reviewerAssignRepo, never()).save(any());
        verify(ideaRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should skip idea if no active reviewers are mapped for the category and stage")
    void assignSubmittedIdeas_NoReviewersMapped() {
        when(ideaRepo.findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus.SUBMITTED))
                .thenReturn(List.of(testIdea));
        when(reviewerCategoryRepo.findActiveReviewerUserIdsByCategoryAndStage(101, 1))
                .thenReturn(Collections.emptyList());

        reviewerAssignmentService.assignSubmittedIdeasEndOfDay();

        verify(reviewerAssignRepo, never()).save(any());
        // Status should remain SUBMITTED
        assertThat(testIdea.getIdeaStatus()).isEqualTo(Constants.IdeaStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Should successfully assign reviewers and move idea to UNDERREVIEW")
    void assignSubmittedIdeas_Success() {
        // Arrange
        User reviewer = new User();
        reviewer.setUserId(200);
        
        when(ideaRepo.findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus.SUBMITTED))
                .thenReturn(List.of(testIdea));
        when(reviewerCategoryRepo.findActiveReviewerUserIdsByCategoryAndStage(101, 1))
                .thenReturn(List.of(200));
        when(userRepo.findAllById(List.of(200)))
                .thenReturn(List.of(reviewer));
        when(reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStage(500, 200, 1))
                .thenReturn(false);

        // Act
        reviewerAssignmentService.assignSubmittedIdeasEndOfDay();

        // Assert
        // Check if assignment was saved
        verify(reviewerAssignRepo, times(1)).save(any(AssignedReviewerToIdea.class));
        
        // Check if idea status and stage were updated
        ArgumentCaptor<Idea> ideaCaptor = ArgumentCaptor.forClass(Idea.class);
        verify(ideaRepo).save(ideaCaptor.capture());
        assertThat(ideaCaptor.getValue().getIdeaStatus()).isEqualTo(Constants.IdeaStatus.UNDERREVIEW);
        assertThat(ideaCaptor.getValue().getStage()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not duplicate assignment if reviewer was ever assigned before")
    void assignSubmittedIdeas_AlreadyAssigned() {
        User reviewer = new User();
        reviewer.setUserId(200);

        when(ideaRepo.findByIdeaStatusAndDeletedFalse(Constants.IdeaStatus.SUBMITTED))
                .thenReturn(List.of(testIdea));
        when(reviewerCategoryRepo.findActiveReviewerUserIdsByCategoryAndStage(101, 1))
                .thenReturn(List.of(200));
        when(userRepo.findAllById(List.of(200)))
                .thenReturn(List.of(reviewer));
        // Simulate already assigned
        when(reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStage(500, 200, 1))
                .thenReturn(true);

        reviewerAssignmentService.assignSubmittedIdeasEndOfDay();

        verify(reviewerAssignRepo, never()).save(any(AssignedReviewerToIdea.class));
    }
}