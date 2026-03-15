package com.ideatrack.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ideatrack.main.data.Constants.Status;
import com.ideatrack.main.dto.admin.AdminHealthSummaryDto;
import com.ideatrack.main.dto.admin.AdminOverdueReviewerDto;
import com.ideatrack.main.dto.admin.AdminProposalSummaryDto;
import com.ideatrack.main.dto.admin.AdminUpdateUserStatusRequest;
import com.ideatrack.main.service.AdminDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.data.domain.*;
    import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//Done by Vibhuti	//tested 100%//

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(authorities = {"ADMIN", "SUPERADMIN"})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestAdminDashboardController {

    @Autowired private MockMvc mockMvc;

    // Use a local ObjectMapper to avoid depending on Boot’s Jackson bean for now.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean private AdminDashboardService adminService;

    @Test
    @DisplayName("listPendingOrStuckProposals: 200 and pageable with sort")
    void listPendingOrStuckProposals_ok_withSort() throws Exception {
        String status = "pending";
        Long stageId = 7L;
        Long reviewerId = 11L;

        AdminProposalSummaryDto dto = new AdminProposalSummaryDto(
                123, "Edge Caching PoC", "pending", "Stage 2",
                OffsetDateTime.parse("2026-01-01T10:00:00Z")
        );

        Page<AdminProposalSummaryDto> page = new PageImpl<>(
                List.of(dto),
                PageRequest.of(2, 50, Sort.by(Sort.Direction.DESC, "lastUpdatedAt")),
                1
        );

        when(adminService.listPendingOrStuckProposals(eq(status), eq(stageId), eq(reviewerId), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/proposals")
                        .param("status", status)
                        .param("stageId", String.valueOf(stageId))
                        .param("reviewerId", String.valueOf(reviewerId))
                        .param("page", "2")
                        .param("size", "50")
                        .param("sort", "lastUpdatedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].proposalId", is(123)))
                .andExpect(jsonPath("$.content[0].title", is("Edge Caching PoC")))
                .andExpect(jsonPath("$.content[0].status", is("pending")))
                .andExpect(jsonPath("$.content[0].currentStage", is("Stage 2")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminService).listPendingOrStuckProposals(eq(status), eq(stageId), eq(reviewerId), captor.capture());

        Pageable pr = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(2, pr.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(50, pr.getPageSize());
        Sort.Order order = pr.getSort().getOrderFor("lastUpdatedAt");
        org.junit.jupiter.api.Assertions.assertNotNull(order);
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, order.getDirection());
    }
    
    @Test
    @DisplayName("listPendingOrStuckProposals: default paging without sort")
    void listPendingOrStuckProposals_defaultPaging_noSort() throws Exception {
        Page<AdminProposalSummaryDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(adminService.listPendingOrStuckProposals(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/proposals")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminService).listPendingOrStuckProposals(isNull(), isNull(), isNull(), captor.capture());

        Pageable captured = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(0, captured.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(20, captured.getPageSize());
        org.junit.jupiter.api.Assertions.assertTrue(captured.getSort().isUnsorted());
    }

    @Test
    @DisplayName("listPendingOrStuckProposals: validation errors for page<0 and size<1 -> 400")
    void listPendingOrStuckProposals_validationErrors() throws Exception {
        mockMvc.perform(get("/api/admin/proposals").param("page", "-1").param("size", "10"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/proposals").param("page", "0").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getOverdueReviewers: returns Page payload and pageable sort works")
    void getOverdueReviewers_ok_withSort() throws Exception {
        AdminOverdueReviewerDto r = new AdminOverdueReviewerDto(10L, 3, 5);

        Page<AdminOverdueReviewerDto> page = new PageImpl<>(
                List.of(r), PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "overdueByDays")), 6
        );

        when(adminService.getOverdueReviewers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/reviewers/overdue")
                        .param("page", "1").param("size", "5").param("sort", "overdueByDays,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewerId", is(10)))
                .andExpect(jsonPath("$.totalElements", is(6)));

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(adminService).getOverdueReviewers(cap.capture());
        Pageable pr = cap.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(1, pr.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(5, pr.getPageSize());
        Sort.Order order = pr.getSort().getOrderFor("overdueByDays");
        org.junit.jupiter.api.Assertions.assertNotNull(order);
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @DisplayName("getOverdueReviewers: validation errors for paging -> 400")
    void getOverdueReviewers_validationErrors() throws Exception {
        mockMvc.perform(get("/api/admin/reviewers/overdue").param("page", "-1").param("size", "5"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/reviewers/overdue").param("page", "0").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getHealthSummary: returns 200 with summary DTO")
    void getHealthSummary_ok() throws Exception {
        AdminHealthSummaryDto summary = AdminHealthSummaryDto.builder()
                .database("healthy").queueBacklog(0).serviceUptime("99.99%")
                .pendingJobs(7).version("v1.0.0").timestamp(OffsetDateTime.now()).build();

        when(adminService.getHealthSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/admin/health/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database", is("healthy")))
                .andExpect(jsonPath("$.queueBacklog", is(0)))
                .andExpect(jsonPath("$.serviceUptime", is("99.99%")))
                .andExpect(jsonPath("$.pendingJobs", is(7)))
                .andExpect(jsonPath("$.version", is("v1.0.0")))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(adminService).getHealthSummary();
    }

    @Test
    @DisplayName("updateUserStatus: returns 200 and delegates to service")
    void updateUserStatus_ok() throws Exception {
        Integer userId = 42;

        AdminUpdateUserStatusRequest req = AdminUpdateUserStatusRequest.builder()
                .status(Status.ACTIVE).build();

        mockMvc.perform(patch("/api/admin/users/{userId}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(adminService).updateUserStatus(eq(userId),
                argThat(r -> r.getStatus() == Status.ACTIVE));
    }


}