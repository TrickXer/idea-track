package com.ideatrack.main.service;

import com.ideatrack.main.data.*;
import com.ideatrack.main.dto.*;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;
import com.ideatrack.main.service.NotificationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestUserActivityService {

    @Mock
    private IIdeaRepository ideaRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserActivityRepository userActivityRepository;

    @Mock
    private GamificationService gamificationService;

    @Mock
    private NotificationHelper notificationHelper;

    @InjectMocks
    private UserActivityService userActivityService;

    private User testUser;
    private Idea testIdea;
    private final String TEST_EMAIL = "test@ideatrack.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setName("John Doe");
        testUser.setEmail(TEST_EMAIL);

        testIdea = new Idea();
        testIdea.setIdeaId(100);
        testIdea.setUser(testUser); // Connect user to idea
    }

    // -------------------------
    // 1. Comment Tests
    // -------------------------

    @Test
    @DisplayName("Add Comment - Success Path")
    public void testAddComment_Success() {
        String text = "Great idea!";
        when(ideaRepository.findById(100)).thenReturn(Optional.of(testIdea));
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gamificationService.getDeltaForActivity(Constants.ActivityType.COMMENT)).thenReturn(5);
        
        when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(i -> {
            UserActivity activity = i.getArgument(0);
            activity.setUserActivityId(10); // Mocking DB generated ID
            return activity;
        });

        CommentDTO result = userActivityService.addComment(100, text, TEST_EMAIL);

        assertNotNull(result);
        assertEquals(text, result.getCommentText());
        assertEquals("John Doe", result.getDisplayName());
        verify(gamificationService).applyDelta(1, 5); 
        verify(userActivityRepository).save(any(UserActivity.class));
    }

    // -------------------------
    // 2. Voting Tests
    // -------------------------

    @Test
    @DisplayName("Cast Vote - New Vote")
    public void testCastVote_NewVote() {
        when(ideaRepository.findById(100)).thenReturn(Optional.of(testIdea));
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        
        // Match specific finder with Idea and User
        when(userActivityRepository.findFirstByIdea_IdeaIdAndUser_UserIdAndActivityTypeAndDeletedFalse(
                100, 1, Constants.ActivityType.VOTE)).thenReturn(Optional.empty());
        
        when(gamificationService.getDeltaForActivity(Constants.ActivityType.VOTE)).thenReturn(2);
        when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityResultResponse result = userActivityService.castVote(100, Constants.VoteType.UPVOTE, TEST_EMAIL);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        verify(gamificationService).applyDelta(1, 2);
    }

    @Test
    @DisplayName("Remove Vote - Success Path")
    public void testRemoveVote_Success() {
        UserActivity existingVote = new UserActivity();
        existingVote.setUserActivityId(55);
        existingVote.setUser(testUser);

        when(ideaRepository.findById(100)).thenReturn(Optional.of(testIdea));
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        
        when(userActivityRepository.findFirstByIdea_IdeaIdAndUser_UserIdAndActivityTypeAndDeletedFalse(
                100, 1, Constants.ActivityType.VOTE)).thenReturn(Optional.of(existingVote));

        userActivityService.removeVote(100, TEST_EMAIL);

        assertTrue(existingVote.isDeleted());
        verify(gamificationService).undoXPChange(1, 55);
    }

    // -------------------------
    // 3. Save Tests (Matching your unchanged Service logic)
    // -------------------------

//    @Test
//    @DisplayName("Toggle Save - Set to True")
//    public void testToggleSave_True() {
//        when(ideaRepository.findById(100)).thenReturn(Optional.of(testIdea));
//        
//        // Service does NOT use User ID for saving currently
//        when(userActivityRepository.findFirstByIdea_IdeaIdAndActivityTypeAndDeletedFalse(
//                100, Constants.ActivityType.SAVE)).thenReturn(Optional.empty());
//
//        userActivityService.toggleSave(100, true, TEST_EMAIL);
//
//        verify(userActivityRepository).save(argThat(activity ->
//                activity.isSavedIdea() && !activity.isDeleted()
//        ));
//    }
//
//    @Test
//    @DisplayName("Toggle Save - Set to False")
//    public void testToggleSave_False() {
//        UserActivity existingSave = new UserActivity();
//        
//        when(ideaRepository.findById(100)).thenReturn(Optional.of(testIdea));
//        
//        // Service does NOT use User ID for saving currently
//        when(userActivityRepository.findFirstByIdea_IdeaIdAndActivityTypeAndDeletedFalse(
//                100, Constants.ActivityType.SAVE)).thenReturn(Optional.of(existingSave));
//
//        userActivityService.toggleSave(100, false);
//
//        assertTrue(existingSave.isDeleted());
//        assertFalse(existingSave.isSavedIdea());
//        verify(userActivityRepository).save(existingSave);
//    }

    // -------------------------
    // 4. Other & Exceptions
    // -------------------------

    @Test
    @DisplayName("Get All Comments - Success Path")
    public void testGetAllCommentsForIdea() {
        UserActivity activity = new UserActivity();
        activity.setUser(testUser);
        activity.setCommentText("Awesome!");

        when(userActivityRepository.findAllByIdea_IdeaIdAndActivityTypeAndDeletedFalseOrderByCreatedAtAsc(
                100, Constants.ActivityType.COMMENT)).thenReturn(List.of(activity));

        List<AllCommentsDTO> comments = userActivityService.getAllCommentsForIdea(100);

        assertEquals(1, comments.size());
        assertEquals("John Doe", comments.get(0).getDisplayName());
    }

    @Test
    @DisplayName("Add Comment - Throws IdeaNotFound")
    public void testAddComment_ThrowsIdeaNotFound() {
        when(ideaRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(IdeaNotFound.class, () -> userActivityService.addComment(999, "text", TEST_EMAIL));
    }
}