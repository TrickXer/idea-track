package com.ideatrack.main.service;

import com.ideatrack.main.TestUtil.ReviewerTestData;
import com.ideatrack.main.data.*;
import com.ideatrack.main.exception.ReviewerBadRequestException;
import com.ideatrack.main.exception.ReviewerForbiddenException;
import com.ideatrack.main.exception.ReviewerNotFoundException;
import com.ideatrack.main.repository.*;
import com.ideatrack.main.service.NotificationHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestReviewerDiscussionService {

    @Mock private IIdeaRepository ideaRepo;
    @Mock private IAssignedReviewerToIdeaRepository reviewerAssignRepo;
    @Mock private IUserActivityRepository activityRepo;
    @Mock private IUserRepository userRepo;
    @Mock private GamificationService gamificationService;
    @Mock private NotificationHelper notificationHelper;

    @InjectMocks
    private ReviewerDiscussionService discussionService;

    private Category category;
    private User owner;
    private Idea ideaStage1;
    private User reviewer;

    @BeforeEach
    void setup() {
        category = ReviewerTestData.categoryWithStages("Process Improvement", 3);
        owner = ReviewerTestData.employee("Employee Owner");
        ideaStage1 = ReviewerTestData.ideaUnderReviewStage(category, owner, 1);

        reviewer = ReviewerTestData.reviewer("Reviewer Alice");
    }

    @Test
    @DisplayName("postDiscussion -> throws when idea not found")
    void postDiscussion_ideaNotFound() {
        when(ideaRepo.findById(ideaStage1.getIdeaId())).thenReturn(Optional.empty());

        assertThrows(ReviewerNotFoundException.class, () ->
                discussionService.postDiscussion(ideaStage1.getIdeaId(), reviewer.getUserId(), 1, "hi", null));
    }

    @Test
    @DisplayName("postDiscussion -> throws when stage mismatch (only current stage allowed)")
    void postDiscussion_stageMismatch() {
        ideaStage1.setStage(2); // mismatch with request stageId=1

        when(ideaRepo.findById(ideaStage1.getIdeaId())).thenReturn(Optional.of(ideaStage1));
        when(userRepo.findById(reviewer.getUserId())).thenReturn(Optional.of(reviewer));

        assertThrows(ReviewerBadRequestException.class, () ->
                discussionService.postDiscussion(ideaStage1.getIdeaId(), reviewer.getUserId(), 1, "Need details", null));
    }

    @Test
    @DisplayName("postDiscussion -> throws when user is not REVIEWER")
    void postDiscussion_onlyReviewerAllowed() {
        User employee = ReviewerTestData.employee("Employee X");

        when(ideaRepo.findById(ideaStage1.getIdeaId())).thenReturn(Optional.of(ideaStage1));
        when(userRepo.findById(employee.getUserId())).thenReturn(Optional.of(employee));

        assertThrows(ReviewerForbiddenException.class, () ->
                discussionService.postDiscussion(ideaStage1.getIdeaId(), employee.getUserId(), 1, "hi", null));
    }

    @Test
    @DisplayName("postDiscussion -> throws when reviewer not assigned to idea/stage")
    void postDiscussion_notAssigned() {
        when(ideaRepo.findById(ideaStage1.getIdeaId())).thenReturn(Optional.of(ideaStage1));
        when(userRepo.findById(reviewer.getUserId())).thenReturn(Optional.of(reviewer));

        when(reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
                ideaStage1.getIdeaId(), reviewer.getUserId(), 1)).thenReturn(false);

        assertThrows(ReviewerForbiddenException.class, () ->
                discussionService.postDiscussion(ideaStage1.getIdeaId(), reviewer.getUserId(), 1, "Need ROI", null));
    }

    @Test
    @DisplayName("postDiscussion -> throws when replyParent invalid")
    void postDiscussion_invalidReplyParent() {
        when(ideaRepo.findById(ideaStage1.getIdeaId())).thenReturn(Optional.of(ideaStage1));
        when(userRepo.findById(reviewer.getUserId())).thenReturn(Optional.of(reviewer));

        when(reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
                ideaStage1.getIdeaId(), reviewer.getUserId(), 1)).thenReturn(true);

        when(activityRepo.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ReviewerBadRequestException.class, () ->
                discussionService.postDiscussion(ideaStage1.getIdeaId(), reviewer.getUserId(), 1, "replying", 9999));
    }

    @Test
    @DisplayName("postDiscussion -> success saves UserActivity and applies XP")
    void postDiscussion_success() {
        when(ideaRepo.findById(ideaStage1.getIdeaId())).thenReturn(Optional.of(ideaStage1));
        when(userRepo.findById(reviewer.getUserId())).thenReturn(Optional.of(reviewer));

        when(reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
                ideaStage1.getIdeaId(), reviewer.getUserId(), 1)).thenReturn(true);

        when(gamificationService.getDeltaForActivity(Constants.ActivityType.REVIEWDISCUSSION)).thenReturn(2);

        discussionService.postDiscussion(ideaStage1.getIdeaId(), reviewer.getUserId(), 1, "Need ROI details", null);

        verify(activityRepo).save(any(UserActivity.class));
        verify(gamificationService).applyDelta(reviewer.getUserId(), 2);
    }
}
