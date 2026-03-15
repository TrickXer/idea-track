package com.ideatrack.main.controller;

import com.ideatrack.main.dto.analytics.*;
import com.ideatrack.main.service.AnalyticService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestAnalyticsController {

    @Autowired
    private AnalyticsController analyticsController;

    @MockitoBean
    private AnalyticService analyticService;

    
    
    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /test - Success")
    void testTestEndpoint() {
        ResponseEntity<String> response = analyticsController.test();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Test API Endpoint called");
    }

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /totalIdeasSubmitted/{year} - Success")
    void testTotalIdeasSubmitted() {
        Mockito.when(analyticService.getTotalIdeaCount(2024)).thenReturn(150L);

        ResponseEntity<Long> response = analyticsController.totalIdeasSubmitted(2024);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(150L);
    }

    
// 	------------------------------- REPORT ENDPOINTS ----------------------------------------
    
    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("GET /getReportList - Success")
    void testGetReportList() {
        List<ReportDTO> mockList = List.of(new ReportDTO());
        Mockito.when(analyticService.getReportList(2024, 5)).thenReturn(mockList);

        ResponseEntity<List<ReportDTO>> response = analyticsController.getReportList(2024, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
    

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("GET /getReportById/{id} - Success")
    void testGetReportById() {
        ReportDTO mockReport = new ReportDTO();
        Mockito.when(analyticService.getReportById(1)).thenReturn(mockReport);

        ResponseEntity<ReportDTO> response = analyticsController.getReportById(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("POST /generateReport - Success")
    void testGenerateReport() {
        GenerateReportDTO req = new GenerateReportDTO();
        Mockito.when(analyticService.generateReport(any(GenerateReportDTO.class))).thenReturn(101);

        ResponseEntity<String> response = analyticsController.generateReport(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("101");
    }
    
    
   
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("DELETE /deleteReportById/{reportId} - Success")
    void testDeleteReportById() {
        Integer reportId = 101;
        
        doNothing().when(analyticService).deleteReportById(reportId);

        ResponseEntity<String> response = analyticsController.deleteReportById(reportId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Report deleted successfully with report ID: " + reportId);
        
        Mockito.verify(analyticService, Mockito.times(1)).deleteReportById(reportId);
    }
    
    

    
// 	------------------------------- EMPLOYEE ENDPOINTS ----------------------------------------
    
    
    
    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /getYearWiseEngagement - Success")
    void testGetYearWiseEngagement() {
        Mockito.when(analyticService.getYearlyEngagement(eq(1), anyInt(), anyInt()))
                .thenReturn(List.of(new YearlyEngagementDTO()));

        ResponseEntity<List<YearlyEngagementDTO>> response = analyticsController.getYearWiseEngagement(1, 2020, 2024);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /getMonthWiseEngagement - Success")
    void testGetMonthWiseEngagement() {
        Mockito.when(analyticService.getMonthlyEngagement(eq(1), anyInt()))
                .thenReturn(List.of(new MonthlyEngagementDTO()));

        ResponseEntity<List<MonthlyEngagementDTO>> response = analyticsController.getMonthWiseEngagement(1, 2024);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
    

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /getPerformanceEmployee/{userId} - Success")
    void testGetPerformanceEmployee() {
        Mockito.when(analyticService.getPerfomanceEmployee(1))
                .thenReturn(List.of(new EmployeePerformanceDTO()));

        ResponseEntity<List<EmployeePerformanceDTO>> response = analyticsController.getPerformanceEmployee(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /getIdeaDistribution/{userId} - Success")
    void testGetIdeaDistribution() {
        Mockito.when(analyticService.getIdeaDistribution(1))
                .thenReturn(List.of(new CategoryCountDTO()));

        ResponseEntity<List<CategoryCountDTO>> response = analyticsController.getIdeaDistribution(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    
    
    
// 	------------------------------- REVIEWER ENDPOINTS ----------------------------------------

    
    

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "REVIEWER"})
    @DisplayName("GET /getPerformanceReviewer/{userId} - Success")
    void testGetPerformanceReviewer() {
        Mockito.when(analyticService.getPerfomanceReviewer(eq(1), anyInt()))
                .thenReturn(List.of(new ReviewerPerformanceDTO()));

        ResponseEntity<List<ReviewerPerformanceDTO>> response = analyticsController.getPerformanceReviewer(1, 2024);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    

    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "REVIEWER"})
    @DisplayName("GET /getReviewerAcceptanceRate/{userId} - Success")
    void testGetReviewerAcceptanceRate() {
        Mockito.when(analyticService.getReviewerAcceptanceCount(eq(1), anyInt()))
                .thenReturn(List.of(new AcceptApproveCountDTO()));

        ResponseEntity<List<AcceptApproveCountDTO>> response = analyticsController.getReviewerAcceptanceCount(1, 2024);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "REVIEWER"})
    @DisplayName("GET /getReviewerDecisionBreakdown/{userId} - Success")
    void testGetReviewerDecisionBreakdown() {
        Mockito.when(analyticService.getReviewerDecisionBreakdown(eq(1), anyInt()))
                .thenReturn(List.of(new DecisionBreakdownDTO()));

        ResponseEntity<List<DecisionBreakdownDTO>> response = analyticsController.getReviewerDecisionBreakdown(1, 2024);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    
    
    
// 	------------------------------- ADMIN ENDPOINTS ----------------------------------------

    
    
    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("GET /getProjectApprovalMetrics - Success")
    void testGetProjectApprovalMetrics() {
        Mockito.when(analyticService.getProjectApprovalMetrics(2024))
                .thenReturn(List.of(new ProjectApprovalMetricsDTO()));

        ResponseEntity<List<ProjectApprovalMetricsDTO>> response = analyticsController.getProjectApprovalMetrics(2024);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("GET /getApprovedIdeaDistribution - Success")
    void testGetApprovedIdeaDistribution() {
        Mockito.when(analyticService.getApprovedIdeaDistribution(2024))
                .thenReturn(List.of(new CategoryCountDTO()));

        ResponseEntity<List<CategoryCountDTO>> response = analyticsController.getApprovedIdeaDistribution(2024);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN"})
    @DisplayName("GET /getDepartmentStatistics - Success")
    void testGetDepartmentStatistics() {
        Mockito.when(analyticService.getDepartmentStatistics(2024, 6))
                .thenReturn(List.of(new DepartmentMetricsDTO()));

        ResponseEntity<List<DepartmentMetricsDTO>> response = analyticsController.getDepartmentStatistics(2024, 6);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    
    
// 	------------------------------- LEADERBOARD ENDPOINTS ----------------------------------------
    
    
    
    @Test
    @WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
    @DisplayName("GET /getLeaderboard/{userId} - Success")
    void testGetLeaderboard() {
        Mockito.when(analyticService.getLeaderboard(1))
                .thenReturn(List.of(new LeaderboardDTO()));

        ResponseEntity<List<LeaderboardDTO>> response = analyticsController.getLeaderboard(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}