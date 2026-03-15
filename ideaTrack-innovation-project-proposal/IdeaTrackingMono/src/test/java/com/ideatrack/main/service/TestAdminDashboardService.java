package com.ideatrack.main.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideatrack.main.controller.AdminDashBoardController;
import com.ideatrack.main.data.Constants.Status;
import com.ideatrack.main.dto.admin.AdminHealthSummaryDto;
import com.ideatrack.main.dto.admin.AdminOverdueReviewerDto;
import com.ideatrack.main.dto.admin.AdminUpdateUserStatusRequest;
import org.springframework.http.MediaType;
import org.mockito.junit.jupiter.MockitoExtension;

//Done By Vibhuti //tested 100%//

@ExtendWith(MockitoExtension.class)
class TestAdminDashboardService {

	    private MockMvc mockMvc;
	    private AdminDashboardService adminService;
	    private ObjectMapper objectMapper;

	    @BeforeEach
	    void setup() {
	        adminService = mock(AdminDashboardService.class);
	        AdminDashBoardController controller = new AdminDashBoardController(adminService);
	        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	        objectMapper = new ObjectMapper();
	    }

	    // GET /api/admin/proposals
	    @Test
	    void listPendingOrStuckProposals_shouldPassQueryParamsAndBuildPageable_descSort() throws Exception {
	        // Given
	        String status = "underreview";
	        Long stageId = 2L;
	        Long reviewerId = 5L;
	        Pageable expected = PageRequest.of(1, 15, Sort.by(Sort.Direction.DESC, "updatedAt"));
	        Page<?> empty = Page.empty(expected);
	        when(adminService.listPendingOrStuckProposals(eq(status), eq(stageId), eq(reviewerId), any(Pageable.class)))
	                .thenReturn((Page) empty);

	        // When
	        mockMvc.perform(get("/api/admin/proposals")
	                        .param("status", status)
	                        .param("stageId", stageId.toString())
	                        .param("reviewerId", reviewerId.toString())
	                        .param("page", "1")
	                        .param("size", "15")
	                        .param("sort", "updatedAt,desc"))
	                .andExpect(MockMvcResultMatchers.status().isOk());

	        // Then
	        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
	        verify(adminService).listPendingOrStuckProposals(eq(status), eq(stageId), eq(reviewerId), pageableCap.capture());

	        Pageable actual = pageableCap.getValue();
	        assertEquals(expected.getPageNumber(), actual.getPageNumber());
	        assertEquals(expected.getPageSize(), actual.getPageSize());
	        assertEquals(expected.getSort(), actual.getSort());
	    }

	    @Test
	    void listPendingOrStuckProposals_shouldDefaultAscSortWhenOnlyFieldProvided() throws Exception {
	        Pageable expected = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "updatedAt"));
	        when(adminService.listPendingOrStuckProposals(any(), any(), any(), any()))
	                .thenReturn((Page) Page.empty(expected));

	        mockMvc.perform(get("/api/admin/proposals")
	                        .param("status", "accepted")
	                        .param("stageId", "10")
	                        .param("reviewerId", "11")
	                        .param("sort", "updatedAt")) // ASC by default
	                .andExpect(status().isOk());

	        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
	        verify(adminService).listPendingOrStuckProposals(eq("accepted"), eq(10L), eq(11L), pageableCap.capture());

	        Pageable actual = pageableCap.getValue();
	        assertEquals(expected.getPageNumber(), actual.getPageNumber());
	        assertEquals(expected.getPageSize(), actual.getPageSize());
	        assertEquals(expected.getSort(), actual.getSort());
	    }

	    @Test
	    void listPendingOrStuckProposals_shouldUseDefaultsWhenNoSortProvided() throws Exception {
	        Pageable expected = PageRequest.of(0, 20); // defaults in controller @RequestParam
	        when(adminService.listPendingOrStuckProposals(any(), any(), any(), any()))
	                .thenReturn((Page) Page.empty(expected));

	        mockMvc.perform(get("/api/admin/proposals"))
	               .andExpect(status().isOk());

	        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
	        verify(adminService).listPendingOrStuckProposals(isNull(), isNull(), isNull(), pageableCap.capture());

	        Pageable actual = pageableCap.getValue();
	        assertEquals(expected.getPageNumber(), actual.getPageNumber());
	        assertEquals(expected.getPageSize(), actual.getPageSize());
	        assertTrue(actual.getSort().isUnsorted());
	    }

	    // GET /api/admin/reviewers/overdue
	    @Test
	    void getOverdueReviewers_shouldBuildPageableAndReturnOk() throws Exception {
	        Pageable expected = PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "reviewerId"));
	        Page<AdminOverdueReviewerDto> page = new PageImpl<>(List.of(), expected, 0);
	        when(adminService.getOverdueReviewers(any())).thenReturn(page);

	        mockMvc.perform(get("/api/admin/reviewers/overdue")
	                        .param("page", "2")
	                        .param("size", "5")
	                        .param("sort", "reviewerId,desc"))
	               .andExpect(status().isOk());

	        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
	        verify(adminService).getOverdueReviewers(pageableCap.capture());

	        Pageable actual = pageableCap.getValue();
	        assertEquals(expected.getPageNumber(), actual.getPageNumber());
	        assertEquals(expected.getPageSize(), actual.getPageSize());
	        assertEquals(expected.getSort(), actual.getSort());
	    }

	    // GET /api/admin/health/summary
	    @Test
	    void getHealthSummary_shouldReturnDtoJson() throws Exception {
	        AdminHealthSummaryDto dto = AdminHealthSummaryDto.builder()
	                .database("healthy")
	                .queueBacklog(0)
	                .serviceUptime("99.99%")
	                .pendingJobs(42)
	                .version("1.0.0")
	                .timestamp(OffsetDateTime.now())
	                .build();

	        when(adminService.getHealthSummary()).thenReturn(dto);

	        mockMvc.perform(get("/api/admin/health/summary"))
	               .andExpect(status().isOk())
	               .andExpect(content().contentTypeCompatibleWith("application/json")) // uses string	               .andExpect(jsonPath("$.database").value("healthy"))
	               .andExpect(jsonPath("$.queueBacklog").value(0))
	               .andExpect(jsonPath("$.serviceUptime").value("99.99%"))
	               .andExpect(jsonPath("$.pendingJobs").value(42))
	               .andExpect(jsonPath("$.version").value("1.0.0"))
	               .andExpect(jsonPath("$.timestamp").exists());

	        verify(adminService).getHealthSummary();
	    }

	 // PATCH /api/admin/users/{userId}/status
	    @Test
	    void updateUserStatus_shouldPassBodyToService_andReturnOk() throws Exception {
	        Integer userId = 123;
	        AdminUpdateUserStatusRequest body = AdminUpdateUserStatusRequest.builder()
	                .status(Status.ACTIVE)
	                .build();

	        mockMvc.perform(patch("/api/admin/users/{userId}/status", userId)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(body)))
	            .andExpect(status().isOk())
	            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
	            .andExpect(jsonPath("$.message").value("User status updated"));
	    }
}	    