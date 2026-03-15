package com.ideatrack.main.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ideatrack.main.dto.objective.ObjectivesResponse;
import com.ideatrack.main.dto.reviewer.ProposalDecisionRequest;
import com.ideatrack.main.service.ObjectiveService;
import com.ideatrack.main.service.ProposalReviewService;

//Done By Vibhuti	//tested 100%//

@SpringBootTest
@WithMockUser(authorities = {"ADMIN", "SUPERADMIN"})  // applies to all tests in this class
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestReviewerController_ProposalReview {

    @Autowired
    private ProposalReviewerController controller;

    @MockitoBean
    private ProposalReviewService proposalReviewService;

    @MockitoBean
    private ObjectiveService objectiveService;


    // -----------------------------------------------------------------------
    // 1) POST /api/reviewer/proposal/{proposalId}/start
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("POST /proposal/{proposalId}/start - returns 200 OK and calls ProposalReviewService.startProposalReview")
    void startProposalReview_ok() {
        Integer proposalId = 101;

        // Act
        ResponseEntity<String> resp = controller.startProposalReview(proposalId);

        // Assert
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo("Proposal review started (UNDERREVIEW:PROPOSAL).");
        verify(proposalReviewService).startProposalReview(proposalId);
        verifyNoMoreInteractions(proposalReviewService);
    }

    // -----------------------------------------------------------------------
    // 2) POST /api/reviewer/proposal/{proposalId}/decision
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("POST /proposal/{proposalId}/decision - returns 200 OK and calls ProposalReviewService.processDecision")
    void processProposalDecision_ok() {
        Integer proposalId = 202;
        ProposalDecisionRequest request = mock(ProposalDecisionRequest.class); // record/POJO safe

        // Act
        ResponseEntity<String> resp = controller.processProposalDecision(proposalId, request);

        // Assert
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo("Proposal decision processed.");

        // Verify exact args
        verify(proposalReviewService).processDecision(eq(proposalId), eq(request));
        verifyNoMoreInteractions(proposalReviewService);
    }

    // -----------------------------------------------------------------------
    // 3) GET /api/reviewer/proposals/{proposalId}/review  (default paging & sort)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("GET /proposals/{proposalId}/review - default paging: page=1,size=20,sort=objectiveSeq,asc")
    void getReviewObjectives_defaults_ok() {
        Integer proposalId = 303;

        // Stub ObjectiveService to return a simple page
        ObjectivesResponse row = mock(ObjectivesResponse.class);
        Page<ObjectivesResponse> page = new PageImpl<>(List.of(row));
        when(objectiveService.getForReview(anyInt(), any(), any(), any(), anyString(), any(Pageable.class)))
                .thenReturn(page);

        // Defaults: hasProof=null, proofType=null, mandatory=null, search="", page=1 (1-based), pageSize=20, sort="objectiveSeq,asc"
        Page<ObjectivesResponse> out = controller.getReviewObjectives(
                proposalId, null, null, null, "", 1, 20, "objectiveSeq,asc"
        );

        assertThat(out).isSameAs(page);

        // Capture pageable & verify wiring
        ArgumentCaptor<Integer> idCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Pageable> pgCap = ArgumentCaptor.forClass(Pageable.class);

        verify(objectiveService).getForReview(idCap.capture(), isNull(), isNull(), isNull(), eq(""), pgCap.capture());
        Pageable used = pgCap.getValue();

        // page=1 (API, 1-based) -> zero-based=0
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort().getOrderFor("objectiveSeq")).isNotNull();
        assertThat(used.getSort().getOrderFor("objectiveSeq").isAscending()).isTrue();
        assertThat(idCap.getValue()).isEqualTo(proposalId);
    }

    // -----------------------------------------------------------------------
    // 4) GET /api/reviewer/proposals/{proposalId}/review  (custom paging & sort)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("GET /proposals/{proposalId}/review - custom paging: page=3,size=5,sort=createdAt,desc")
    void getReviewObjectives_customPaging_ok() {
        Integer proposalId = 404;

        when(objectiveService.getForReview(anyInt(), any(), any(), any(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // page=3 -> zero-based=2, size=5, sort=createdAt,desc
        Page<ObjectivesResponse> out = controller.getReviewObjectives(
                proposalId, true, "pdf", true, "proof", 3, 5, "createdAt,desc"
        );

        assertThat(out).isNotNull();

        ArgumentCaptor<Boolean> hasProofCap = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> proofCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> mandatoryCap = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> searchCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pgCap = ArgumentCaptor.forClass(Pageable.class);

        verify(objectiveService).getForReview(
                eq(proposalId),
                hasProofCap.capture(),
                proofCap.capture(),
                mandatoryCap.capture(),
                searchCap.capture(),
                pgCap.capture()
        );

        Pageable used = pgCap.getValue();
        assertThat(used.getPageNumber()).isEqualTo(2); // zero-based
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(used.getSort().getOrderFor("createdAt").isDescending()).isTrue();

        assertThat(hasProofCap.getValue()).isTrue();
        assertThat(proofCap.getValue()).isEqualTo("pdf");
        assertThat(mandatoryCap.getValue()).isTrue();
        assertThat(searchCap.getValue()).isEqualTo("proof");
    }
}