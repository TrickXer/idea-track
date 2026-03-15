package com.ideatrack.main.service;

import com.ideatrack.main.data.*;
import com.ideatrack.main.dto.*;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.*;
import com.ideatrack.main.service.NotificationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestIdeaService {

    @Mock private IIdeaRepository ideaRepository;
    @Mock private IUserRepository userRepository;
    @Mock private ICategoryRepository categoryRepository;
    @Mock private IUserActivityRepository userActivityRepository;
    @Mock private IAssignedReviewerToIdeaRepository reviewerRepository;
    @Mock private GamificationService gamificationService;
    @Mock private NotificationHelper notificationHelper;

    @InjectMocks
    private IdeaService ideaService;

    private User sampleUser;
    private Category sampleCategory;
    private Idea sampleIdea;
    private final String AUTH_EMAIL = "aaditee@ideatrack.com";

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1);
        sampleUser.setName("Aaditee");
        sampleUser.setEmail(AUTH_EMAIL);

        sampleCategory = new Category();
        sampleCategory.setCategoryId(10);
        sampleCategory.setName("Tech");

        sampleIdea = new Idea();
        sampleIdea.setIdeaId(100);
        sampleIdea.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        sampleIdea.setUser(sampleUser);
        sampleIdea.setCategory(sampleCategory);
        sampleIdea.setDeleted(false);
    }

    // -------------------------------------------------------------------------
    // 1. Lifecycle Operations
    // -------------------------------------------------------------------------

    @Test
    void testCreateIdea_Success() {
        IdeaCreateRequest req = new IdeaCreateRequest();
        req.setCategoryId(10);
        req.setTitle("New Idea");

        // Use findByEmail now instead of findById
        when(userRepository.findByEmail(AUTH_EMAIL)).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findById(10)).thenReturn(Optional.of(sampleCategory));
        when(ideaRepository.save(any(Idea.class))).thenAnswer(i -> i.getArgument(0));

        // Pass the email string as the second argument
        IdeaResponse res = ideaService.createIdea(req, AUTH_EMAIL);

        assertEquals("New Idea", res.getTitle());
        verify(userRepository).findByEmail(AUTH_EMAIL);
        verify(ideaRepository).save(any(Idea.class));
    }

    @Test
    void testUpdateIdea_ThrowsException_WhenNotDraft() {
        sampleIdea.setIdeaStatus(Constants.IdeaStatus.SUBMITTED);
        when(ideaRepository.findById(100)).thenReturn(Optional.of(sampleIdea));

        // Note: If you changed your Service to throw IllegalArgumentException, update this line
        assertThrows(IllegalStateException.class, () -> 
            ideaService.updateIdea(100, new IdeaUpdateRequest()));
    }

    @Test
     void testSubmitIdea_Success() throws IdeaNotFound {
        when(ideaRepository.findById(100)).thenReturn(Optional.of(sampleIdea));
        when(gamificationService.getDeltaForIdeaStatus(Constants.IdeaStatus.SUBMITTED)).thenReturn(10);
        when(ideaRepository.save(any(Idea.class))).thenReturn(sampleIdea);

        IdeaResponse response = ideaService.submitIdea(100);

        assertEquals(Constants.IdeaStatus.SUBMITTED, sampleIdea.getIdeaStatus());
        verify(gamificationService).applyDelta(1, 10);
    }

    @Test
    void testSoftDeleteIdea_Success() throws IdeaNotFound {
        sampleIdea.setIdeaStatus(Constants.IdeaStatus.SUBMITTED);
        when(ideaRepository.findById(100)).thenReturn(Optional.of(sampleIdea));

        ideaService.softDeleteIdea(100);

        assertTrue(sampleIdea.isDeleted());
        verify(gamificationService).undoIdeaSubmissionXP(1);
        verify(ideaRepository).save(sampleIdea);
    }

    // -------------------------------------------------------------------------
    // 2. Exception Cases (Updated for Custom Exceptions)
    // -------------------------------------------------------------------------

    @Test
    void testRequireIdea_ThrowsIdeaNotFound() {
        when(ideaRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IdeaNotFound.class, () -> ideaService.getIdea(999, 1));
    }

    @Test
    void testIsOwner_Success() {
        when(ideaRepository.findById(100)).thenReturn(Optional.of(sampleIdea));

        boolean result = ideaService.isOwner(100, AUTH_EMAIL);

        assertTrue(result);
    }
    @Test
    void testIsOwner_Failure_WrongEmail() {
        when(ideaRepository.findById(100)).thenReturn(Optional.of(sampleIdea));

        boolean result = ideaService.isOwner(100, "wrong@email.com");

        assertFalse(result);
    }

    @Test
    void testRequireCategory_ThrowsCategoryNotFound() {
        // 1. Arrange: No userId in req anymore
        IdeaCreateRequest req = new IdeaCreateRequest();
        req.setCategoryId(99);
        req.setTitle("Test Idea");

        // 2. Mocking: Use findByEmail and the AUTH_EMAIL constant
        when(userRepository.findByEmail(AUTH_EMAIL)).thenReturn(Optional.of(sampleUser));
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        // 3. Act & Assert: Pass the email string as the second parameter
        assertThrows(CategoryNotFound.class, () -> ideaService.createIdea(req, AUTH_EMAIL));
        
        // Verify that it looked for the user first, then failed at the category
        verify(userRepository).findByEmail(AUTH_EMAIL);
        verify(categoryRepository).findById(99);
    }

    // -------------------------------------------------------------------------
    // 3. Query Operations
    // -------------------------------------------------------------------------

    @Test
    void testSearchIdeas() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Idea> page = new PageImpl<>(List.of(sampleIdea));

        when(ideaRepository.searchIdeas(any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(page);

        PagedResponse<IdeaResponse> res = ideaService.searchIdeas(
                "test", null, null, null, null, false, pageable, 1);

        assertNotNull(res);
        assertEquals(1, res.getContent().size());
        verify(ideaRepository).searchIdeas(any(), any(), any(), any(), any(), anyBoolean(), any());
    }
}