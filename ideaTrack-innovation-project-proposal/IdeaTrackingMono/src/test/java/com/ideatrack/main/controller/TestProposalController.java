//package com.ideatrack.main.controller;
//
//import com.ideatrack.main.dto.proposal.AcceptedIdeaDashboardDTO;
//import com.ideatrack.main.dto.proposal.ProposalCreateRequestDTO;
//import com.ideatrack.main.dto.proposal.ProposalResponseDTO;
//import com.ideatrack.main.dto.proposal.ProposalUpdateRequestDTO;
//import com.ideatrack.main.service.ProposalService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//// Done By Vibhuti // updated to match controller as of Feb 2026
//
//@SpringBootTest
//@WithMockUser(authorities = {"ADMIN", "SUPERADMIN", "REVIEWER", "EMPLOYEE"}) // applies to all tests in this class
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//public class TestProposalController {
//
//    @Autowired
//    private ProposalController controller;
//
//    @MockitoBean
//    private ProposalService proposalService;
//
//    // 1) POST /api/proposal/ideas/{ideaId}/convert-to-proposal
//    @Test
//    @DisplayName("POST convertToProposal - returns service DTO and calls service with ideaId + body")
//    void convertToProposal_ok() {
//        int ideaId = 1234;
//
//        // Build a VALID request (satisfy @NotNull/@PositiveOrZero constraints)
//        ProposalCreateRequestDTO req = new ProposalCreateRequestDTO();
//        req.setUserId(55);
//        req.setBudget(0L);
//        req.setTimeLineStart(LocalDate.of(2026, 1, 1));
//        req.setTimeLineEnd(LocalDate.of(2026, 12, 31));
//
//        // Expected response from service
//        ProposalResponseDTO expected = new ProposalResponseDTO();
//        expected.setProposalId(1001);
//        expected.setIdeaId(ideaId);
//        expected.setUserId(55);
//
//        // Stub service
//        when(proposalService.convertIdeaToProposal(eq(ideaId), any(ProposalCreateRequestDTO.class)))
//                .thenReturn(expected);
//
//        // Act
//        ProposalResponseDTO out = controller.convertToProposal(ideaId, req);
//
//        // Assert
//        assertThat(out).isNotNull();
//        assertThat(out.getProposalId()).isEqualTo(1001);
//        assertThat(out.getIdeaId()).isEqualTo(ideaId);
//        assertThat(out.getUserId()).isEqualTo(55);
//
//        // Verify exact arguments
//        verify(proposalService).convertIdeaToProposal(eq(ideaId), eq(req));
//        verifyNoMoreInteractions(proposalService);
//    }
//
//    // 2) GET /api/proposal/{userId}/accepted-ideas — simple list and wiring to service
//    @Test
//    @DisplayName("GET accepted ideas with proposal - returns list from service and calls service with userId")
//    void getAcceptedIdeas_ok() {
//        Integer userId = 55;
//
//        // Arrange a simple list (two ideas, one with proposal, one without)
//        AcceptedIdeaDashboardDTO item1 = new AcceptedIdeaDashboardDTO();
//        item1.setIdeaId(101);
//        item1.setIdeaTitle("Smart Traffic System");
//        item1.setIdeaDescription("Sensors for adaptive control");
//        item1.setIdeaStatus("APPROVED");
//        item1.setIdeaCreatedAt(LocalDateTime.of(2026, 1, 10, 10, 0));
//        item1.setProposalId(501);
//        item1.setBudget(300000L);
//        item1.setProposalStatus("DRAFT");
//
//        AcceptedIdeaDashboardDTO item2 = new AcceptedIdeaDashboardDTO();
//        item2.setIdeaId(102);
//        item2.setIdeaTitle("AI Portal");
//        item2.setIdeaDescription("Automates HR requests");
//        item2.setIdeaStatus("APPROVED");
//        item2.setIdeaCreatedAt(LocalDateTime.of(2026, 1, 12, 9, 30));
//        // no proposal for item2 (null fields)
//
//        List<AcceptedIdeaDashboardDTO> expected = List.of(item1, item2);
//
//        when(proposalService.getAcceptedIdeasWithProposal(eq(userId))).thenReturn(expected);
//
//        // Act
//        List<AcceptedIdeaDashboardDTO> out = (List<AcceptedIdeaDashboardDTO>) controller.getAcceptedIdeas(userId);
//
//        // Assert
//        assertThat(out).isNotNull().hasSize(2);
//        assertThat(out).isSameAs(expected);
//
//        verify(proposalService).getAcceptedIdeasWithProposal(eq(userId));
//        verifyNoMoreInteractions(proposalService);
//    }
//
//    // 3) PUT /api/proposal/updateProposal/{proposalId}
//    @Test
//    @DisplayName("PUT updateDraft - returns service DTO and calls service with proposalId + body")
//    void updateDraft_ok() {
//        int proposalId = 900;
//        ProposalUpdateRequestDTO req = new ProposalUpdateRequestDTO();
//        ProposalResponseDTO expected = new ProposalResponseDTO();
//
//        when(proposalService.updateDraft(eq(proposalId), any(ProposalUpdateRequestDTO.class)))
//                .thenReturn(expected);
//
//        ProposalResponseDTO out = controller.updateDraft(proposalId, req);
//
//        assertThat(out).isSameAs(expected);
//        verify(proposalService).updateDraft(eq(proposalId), eq(req));
//        verifyNoMoreInteractions(proposalService);
//    }
//
//    // 4) DELETE /api/proposal/deleteProposal/{proposalId}
//    @Test
//    @DisplayName("DELETE deleteDraft - calls service and returns void")
//    void deleteDraft_ok() {
//        int proposalId = 777;
//
//        controller.deleteDraft(proposalId);
//
//        verify(proposalService).deleteDraft(eq(proposalId));
//        verifyNoMoreInteractions(proposalService);
//    }
//}