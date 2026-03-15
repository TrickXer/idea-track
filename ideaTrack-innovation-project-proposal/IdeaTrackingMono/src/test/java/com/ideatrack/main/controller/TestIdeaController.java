package com.ideatrack.main.controller;
 
import com.ideatrack.main.dto.*;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.service.IdeaService;
import com.ideatrack.main.service.UserActivityService;
import com.ideatrack.main.data.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.HttpStatus;
import com.ideatrack.main.data.Constants;
 
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
 
@WithMockUser(authorities = {"ADMIN", "SUPERADMIN", "REVIEWER", "EMPLOYEE"})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestIdeaController {

    @Autowired
    private IdeaController ideaController;

    @MockitoBean
    private IdeaService ideaService;

    @MockitoBean
    private UserActivityService activityService;

    private Principal mockPrincipal;
    private final String TEST_EMAIL = "test@ideatrack.com";

    @BeforeEach
    void setUp() {
        mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn(TEST_EMAIL);
    }
 
    // -----------------------------------------------------------------------
    // 1. CORE IDEA OPERATIONS
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /insertIdea - Success")
    void testCreateIdea() {
        IdeaCreateRequest req = new IdeaCreateRequest();
        Mockito.when(ideaService.createIdea(any(IdeaCreateRequest.class), eq(TEST_EMAIL)))
               .thenReturn(IdeaResponse.builder().build());
 
        ResponseEntity<String> response = ideaController.createIdea(req, mockPrincipal);
 
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("Idea is inserted successfully");
    }

    @Test
    @DisplayName("GET /{id} - Success")
    void testGetIdea() {
        IdeaResponse mockResponse = IdeaResponse.builder().ideaId(1).title("Test Idea").build();
        Mockito.when(ideaService.getIdea(eq(1), any())).thenReturn(mockResponse);

        ResponseEntity<IdeaResponse> response = ideaController.getIdea(1, 100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Test Idea");
    }

    @Test
    @DisplayName("PUT /editDraft/{id} - Success")
    void testUpdateIdea() {
        IdeaUpdateRequest req = new IdeaUpdateRequest();
        Mockito.when(ideaService.updateIdea(eq(1), any())).thenReturn(IdeaResponse.builder().build());

        ResponseEntity<String> response = ideaController.updateIdea(1, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo("Your changes have been saved in draft");
    }

    @Test
    @DisplayName("POST /submit/{id} - Success")
    void testSubmitIdea() {
        Mockito.when(ideaService.submitIdea(1)).thenReturn(IdeaResponse.builder().build());

        ResponseEntity<String> response = ideaController.submitIdea(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo("The idea is submitted");
    }

    @Test
    @DisplayName("GET /user/{userId} - Success")
    void testGetIdeasByUserId() {
        List<IdeaResponse> mockList = List.of(IdeaResponse.builder().ideaId(1).build());
        Mockito.when(ideaService.getIdeasByUser(10)).thenReturn(mockList);

        ResponseEntity<List<IdeaResponse>> response = ideaController.getIdeasByUserId(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("DELETE /deleteIdea/{id} - Success")
    void testDeleteIdea() {
        ResponseEntity<String> response = ideaController.deleteIdea(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("The idea has been successfully deleted");
        Mockito.verify(ideaService).softDeleteIdea(1);
    }

    @Test
    @DisplayName("GET (Search) - Success")
    void testSearchIdeas() {
        PagedResponse<IdeaResponse> mockPaged = PagedResponse.<IdeaResponse>builder()
                .content(Collections.emptyList()).totalElements(0).build();
        Mockito.when(ideaService.searchIdeas(any(), any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(mockPaged);

        ResponseEntity<PagedResponse<IdeaResponse>> response = ideaController.searchIdeas(
                "AI", null, null, null, null, false, 0, 10, "createdAt,desc", 100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // 2. USER INTERACTIONS (ACTIVITY)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /postComments/{id}/ - Success")
    void testAddComment() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setText("Great Idea");
        Mockito.when(activityService.addComment(anyInt(), anyString(), eq(TEST_EMAIL)))
                .thenReturn(CommentDTO.builder().commentText("Great Idea").build());

        ResponseEntity<CommentDTO> response = ideaController.addComment(1, req, mockPrincipal);
 
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCommentText()).isEqualTo("Great Idea");
    }

    @Test
    @DisplayName("GET /getComments/{id} - Success")
    void testGetComments() {
        List<AllCommentsDTO> mockComments = List.of(new AllCommentsDTO());
        Mockito.when(activityService.getAllCommentsForIdea(1)).thenReturn(mockComments);

        ResponseEntity<List<AllCommentsDTO>> response = ideaController.getComments(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("POST /vote/{id} - Success")
    void testCastVote() {
        VoteRequest req = VoteRequest.builder()
                .voteType(Constants.VoteType.UPVOTE)
                .build();

        ActivityResultResponse mockResult = ActivityResultResponse.builder()
                .ideaId(1)
                .userId(100)
                .voteType(Constants.VoteType.UPVOTE)
                .build();
 
        // Mocking Service: now requires the email parameter
        Mockito.when(activityService.castVote(eq(1), eq(Constants.VoteType.UPVOTE), eq(TEST_EMAIL)))
               .thenReturn(mockResult);

        ResponseEntity<ActivityResultResponse> response = ideaController.castVote(1, req, mockPrincipal);
 
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIdeaId()).isEqualTo(1);
        Mockito.verify(activityService).castVote(1, Constants.VoteType.UPVOTE, TEST_EMAIL);
    }

    @Test
    @DisplayName("DELETE /removeVote/{id} - Success")
    void testRemoveVote() {
        // removeVote in Controller no longer uses userId Param, only Principal
        ResponseEntity<String> response = ideaController.removeVote(1, mockPrincipal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        Mockito.verify(activityService).removeVote(1, TEST_EMAIL);
    }

//    @Test
//    @DisplayName("POST /save/{id} - Save vs Unsave")
//    void testToggleSave() {
//        ResponseEntity<String> saveResponse = ideaController.toggleSave(1, true);
//        assertThat(saveResponse.getBody()).isEqualTo("The idea is saved");
//
//        ResponseEntity<String> unsaveResponse = ideaController.toggleSave(1, false);
//        assertThat(unsaveResponse.getBody()).isEqualTo("Idea removed from saved");
//        
//        // Verify Service call includes the email
//        Mockito.verify(activityService, Mockito.times(2)).toggleSave(eq(1), anyBoolean());
//    }

    // -----------------------------------------------------------------------
    // 3. EXCEPTION SCENARIOS
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /{id} - IdeaNotFound propagation")
    void testGetIdea_NotFound() {
        Mockito.when(ideaService.getIdea(eq(404), any()))
               .thenThrow(new IdeaNotFound("Idea Not Found"));

        assertThrows(IdeaNotFound.class, () -> ideaController.getIdea(404, 1));
    }
}