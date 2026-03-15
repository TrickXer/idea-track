package com.ideatrack.main.controller;

import com.ideatrack.main.dto.PagedResponse;
import com.ideatrack.main.dto.reviewer.ReviewerDashboardDTO;
import com.ideatrack.main.dto.reviewer.ReviewerDecisionRequest;
import com.ideatrack.main.dto.reviewer.ReviewerDiscussionDTO;
import com.ideatrack.main.dto.reviewer.ReviewerDiscussionRequestDTO;
import com.ideatrack.main.service.ReviewerAssignmentService;
import com.ideatrack.main.service.ReviewerDashboardService;
import com.ideatrack.main.service.ReviewerDecisionService;
import com.ideatrack.main.service.ReviewerDiscussionService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "REVIEWER"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestReviewerController {

    @Autowired
    private ReviewerController reviewerController;

    @MockitoBean private ReviewerAssignmentService assignmentService;
    @MockitoBean private ReviewerDashboardService dashboardService;
    @MockitoBean private ReviewerDecisionService decisionService;
    @MockitoBean private ReviewerDiscussionService discussionService;

    // ------------------------------------------------------------
    // 1) POST /api/reviewer/ideas/{ideaId}/decision
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST submitDecision - Success")
    void submitDecision_ok() {
        ReviewerDecisionRequest req = new ReviewerDecisionRequest();
        req.setReviewerId(101);
        req.setDecision("ACCEPTED");
        req.setFeedback("ok");

        // service is void -> just verify called
        ResponseEntity<String> resp = reviewerController.submitDecision(5001, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Decision processed successfully");

        verify(decisionService).processDecision(5001, req);
        verifyNoInteractions(dashboardService, discussionService, assignmentService);
    }

    // ------------------------------------------------------------
    // 1b) GET /api/reviewer/idea/{ideaId}/decisions
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET getDecisions - Success")
    void getDecisions_ok() {
        ReviewerDecisionRequest d1 = new ReviewerDecisionRequest();
        d1.setReviewerId(101);
        d1.setDecision("ACCEPTED");
        d1.setFeedback("good");

        ReviewerDecisionRequest d2 = new ReviewerDecisionRequest();
        d2.setReviewerId(102);
        d2.setDecision("REFINE");
        d2.setFeedback("need more info");

        when(decisionService.getReviewerDecisions(5001)).thenReturn(List.of(d1, d2));

        ResponseEntity<List<ReviewerDecisionRequest>> resp = reviewerController.getDecisions(5001);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
        assertThat(resp.getBody().get(0).getReviewerId()).isEqualTo(101);
        assertThat(resp.getBody().get(0).getDecision()).isEqualTo("ACCEPTED");
        assertThat(resp.getBody().get(1).getDecision()).isEqualTo("REFINE");

        verify(decisionService).getReviewerDecisions(5001);
        verifyNoInteractions(dashboardService, discussionService, assignmentService);
    }

    // ------------------------------------------------------------
    // 2) GET /api/reviewer/me/dashboard?filter=ALL
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET getDashboard - Success")
    void getDashboard_ok() {
        ReviewerDashboardDTO dto = ReviewerDashboardDTO.builder()
                .ideaId(5001)
                .ideaTitle("Idea One")
                .employeeName("Emp One")
                .categoryName("Quality")
                .assignmentStage(1)
                .currentIdeaStatus("UNDERREVIEW")
                .reviewerDecision("UNDERREVIEW")
                .assignedDate(LocalDateTime.now())
                .build();

        when(dashboardService.getReviewerDashboard("ALL")).thenReturn(List.of(dto));

        ResponseEntity<List<ReviewerDashboardDTO>> resp =
                reviewerController.getDashboard("ALL");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getIdeaId()).isEqualTo(5001);
        assertThat(resp.getBody().get(0).getIdeaTitle()).isEqualTo("Idea One");

        verify(dashboardService).getReviewerDashboard("ALL");
        verifyNoInteractions(decisionService, discussionService, assignmentService);
    }

    // ------------------------------------------------------------
    // 3) POST /api/reviewer/ideas/{ideaId}/discussions
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST postDiscussion - Success")
    void postDiscussion_ok() {
        ReviewerDiscussionRequestDTO req = new ReviewerDiscussionRequestDTO();
        req.setUserId(101);
        req.setStageId(1);
        req.setText("Follow up");
        req.setReplyParent(null);

        ResponseEntity<String> resp = reviewerController.postDiscussion(5001, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Discussion posted");

        verify(discussionService).postDiscussion(5001, 101, 1, "Follow up", null);
        verifyNoInteractions(decisionService, dashboardService, assignmentService);
    }

    // ------------------------------------------------------------
    // 4) GET /api/reviewer/ideas/{ideaId}/discussions?stageId=1
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET getDiscussions - Success")
    void getDiscussions_ok() {
        ReviewerDiscussionDTO dto = ReviewerDiscussionDTO.builder()
                .userActivityId(9001)
                .userId(101)
                .displayName("Reviewer One")
                .commentText("Hi")
                .stageId(1)
                .replyParent(null)
                .createdAt(LocalDateTime.now())
                .build();

        when(discussionService.getDiscussionsForStage(5001, 1)).thenReturn(List.of(dto));

        ResponseEntity<List<ReviewerDiscussionDTO>> resp =
                reviewerController.getDiscussions(5001, 1);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getUserActivityId()).isEqualTo(9001);
        assertThat(resp.getBody().get(0).getCommentText()).isEqualTo("Hi");

        verify(discussionService).getDiscussionsForStage(5001, 1);
        verifyNoInteractions(decisionService, dashboardService, assignmentService);
    }

    // ------------------------------------------------------------
    // 5) GET /api/reviewer/ideas/{ideaId}/discussions/page
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET getDiscussionsPaged - Success (verifies pageable params passed)")
    void getDiscussionsPaged_ok() {
        ReviewerDiscussionDTO dto = ReviewerDiscussionDTO.builder()
                .userActivityId(9002)
                .userId(101)
                .displayName("Reviewer One")
                .commentText("Paged comment")
                .stageId(1)
                .replyParent(null)
                .createdAt(LocalDateTime.now())
                .build();

        PagedResponse<ReviewerDiscussionDTO> expected = PagedResponse.<ReviewerDiscussionDTO>builder()
                .content(List.of(dto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(discussionService.getDiscussionsForStagePaged(eq(5001), eq(1), any()))
                .thenReturn(expected);

        ResponseEntity<PagedResponse<ReviewerDiscussionDTO>> resp =
                reviewerController.getDiscussionsPaged(5001, 1, 0, 20, "createdAt,asc");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getContent()).hasSize(1);
        assertThat(resp.getBody().getContent().get(0).getUserActivityId()).isEqualTo(9002);
        assertThat(resp.getBody().getPage()).isEqualTo(0);
        assertThat(resp.getBody().getSize()).isEqualTo(20);

        // capture pageable to validate sort parsing
        var pageableCaptor = Mockito.<org.springframework.data.domain.Pageable>mockingDetails(discussionService)
                .getInvocations()
                .stream()
                .filter(inv -> inv.getMethod().getName().equals("getDiscussionsForStagePaged"))
                .findFirst()
                .map(inv -> (org.springframework.data.domain.Pageable) inv.getArguments()[2])
                .orElse(null);

        assertThat(pageableCaptor).isNotNull();
        assertThat(pageableCaptor.getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);

        verify(discussionService).getDiscussionsForStagePaged(eq(5001), eq(1), any());
        verifyNoInteractions(decisionService, dashboardService, assignmentService);
    }

    // ------------------------------------------------------------
    // 6) POST /api/reviewer/jobs/assignments/eod
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST runEodAssignmentNow - Success (202 Accepted)")
    void runEodAssignmentNow_ok() {
        ResponseEntity<String> resp = reviewerController.runEodAssignmentNow();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resp.getBody()).contains("EOD assignment triggered");

        verify(assignmentService).assignSubmittedIdeasEndOfDay();
        verifyNoInteractions(decisionService, dashboardService, discussionService);
    }

    // ------------------------------------------------------------
    // Optional: sort default behavior (when sort is blank)
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET getDiscussionsPaged - default sort when sortParam blank")
    void getDiscussionsPaged_defaultSort_whenBlank() {
        when(discussionService.getDiscussionsForStagePaged(eq(5001), eq(1), any()))
                .thenReturn(PagedResponse.<ReviewerDiscussionDTO>builder()
                        .content(List.of())
                        .page(0).size(20)
                        .totalElements(0)
                        .totalPages(0)
                        .first(true).last(true)
                        .build());

        reviewerController.getDiscussionsPaged(5001, 1, 0, 20, "   ");

        verify(discussionService).getDiscussionsForStagePaged(eq(5001), eq(1), any(Pageable.class));
    }
}