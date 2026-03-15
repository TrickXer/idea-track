package com.ideatrack.main.service;

import com.ideatrack.main.data.*;
import com.ideatrack.main.dto.analytics.*;
import com.ideatrack.main.exception.*;
import com.ideatrack.main.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestAnalyticsService {

    @Mock private IReportRepository reportRepo;
    @Mock private IIdeaRepository ideaRepo;
    @Mock private IUserRepository userRepo;
    @Mock private IUserActivityRepository userActivityRepo;
    @Mock private IAssignedReviewerToIdeaRepository assignedReviewerRepo;
    @Mock private IDepartmentRepository departmentRepo;
    @Mock private ICategoryRepository categoryRepo;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private AnalyticService analyticService;

    private User sampleUser;
    private Report sampleReport;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1);
        sampleUser.setName("John Doe");
        sampleUser.setTotalXP(500);

        sampleReport = new Report();
        sampleReport.setId(101);
        sampleReport.setDeleted(false);
    }

    // -------------------------------------------------------------------------
    // 1. Report Logic
    // -------------------------------------------------------------------------

    @Test
    void testGetReportList_Success() {
        when(reportRepo.findAllByDeletedFalseAndCreatedAtBetween(any(), any()))
            .thenReturn(List.of(sampleReport));
        when(modelMapper.map(any(), eq(ReportDTO.class))).thenReturn(new ReportDTO());

        List<ReportDTO> result = analyticService.getReportList(2023, 5);
        assertEquals(1, result.size());
    }

    @Test
    void testGetReportById_Success() {
        when(reportRepo.findById(101)).thenReturn(Optional.of(sampleReport));
        when(modelMapper.map(sampleReport, ReportDTO.class)).thenReturn(new ReportDTO());
        assertNotNull(analyticService.getReportById(101));
    }

    @Test
    void testGetReportById_Failure_NotFound() {
        when(reportRepo.findById(99)).thenReturn(Optional.empty());
        assertThrows(ReportNotFoundException.class, () -> analyticService.getReportById(99));
    }

    @Test
    void testGenerateReport_Failure_NullRequest() {
        GenerateReportDTO req = new GenerateReportDTO(); // Null scope/userId
        assertThrows(IllegalArgumentException.class, () -> analyticService.generateReport(req));
    }

    @Test
    void testGenerateReport_Success_DepartmentScope() {
        GenerateReportDTO req = new GenerateReportDTO();
        req.setScope(Constants.Scope.DEPARTMENT);
        req.setUserId(1);
        req.setScopeId(5);

        Department dept = new Department();
        dept.setDeptName("Sales");

        when(userRepo.findById(1)).thenReturn(Optional.of(sampleUser));
        when(departmentRepo.findById(5)).thenReturn(Optional.of(dept));
        when(reportRepo.save(any(Report.class))).thenReturn(sampleReport);

        Integer id = analyticService.generateReport(req);
        assertEquals(101, id);
        verify(ideaRepo).countByCategory_Department_DeptIdAndCreatedAtBetween(eq(5), any(), any());
    }

    @Test
    void testDeleteReportById_Success() {
        when(reportRepo.findById(101)).thenReturn(Optional.of(sampleReport));
        analyticService.deleteReportById(101);
        assertTrue(sampleReport.isDeleted());
        verify(reportRepo).save(sampleReport);
    }

    // -------------------------------------------------------------------------
    // 2. Employee Logic
    // -------------------------------------------------------------------------

    
    @Test
    void testGetTotalIdeaCount_Success() {
        // Arrange
        int year = 2023;
        long expectedCount = 25L;
        
        // We use any() for LocalDateTime because the service creates these objects internally
        when(ideaRepo.countByDeletedFalseAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(expectedCount);

        // Act
        long actualCount = analyticService.getTotalIdeaCount(year);

        // Assert
        assertEquals(expectedCount, actualCount);
        
        // Verify that the repository was called exactly once
        verify(ideaRepo, times(1)).countByDeletedFalseAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetTotalIdeaCount_ZeroResult() {
        // Arrange
        int year = 2025;
        when(ideaRepo.countByDeletedFalseAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(0L);

        // Act
        long actualCount = analyticService.getTotalIdeaCount(year);

        // Assert
        assertEquals(0L, actualCount);
        verify(ideaRepo).countByDeletedFalseAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    
    
    @Test
    void testGetYearlyEngagement_Success() {
        List<YearlyEngagementDTO> list = analyticService.getYearlyEngagement(1, 2022, 2023);
        assertEquals(2, list.size());
        verify(userActivityRepo, atLeastOnce()).countByUser_UserIdAndActivityTypeAndCreatedAtBetween(any(), any(), any(), any());
    }

    
    
    @Test
    void testGetMonthlyEngagement_Success() {
        Integer userId = 1;
        int year = 2023;
        
        when(ideaRepo.countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(eq(userId), any(), any()))
            .thenReturn(5L);
        when(userActivityRepo.countByUser_UserIdAndActivityTypeAndCreatedAtBetween(eq(userId), eq(Constants.ActivityType.VOTE), any(), any()))
            .thenReturn(10L);
        when(userActivityRepo.countByUser_UserIdAndActivityTypeAndCreatedAtBetween(eq(userId), eq(Constants.ActivityType.COMMENT), any(), any()))
            .thenReturn(3L);

        List<MonthlyEngagementDTO> result = analyticService.getMonthlyEngagement(userId, year);

        assertNotNull(result);
        assertEquals(12, result.size()); // Verify we have data for all 12 months
        
        assertEquals("JANUARY", result.get(0).getMonth());
        assertEquals(5L, result.get(0).getIdeaCount());
        assertEquals(10L, result.get(0).getVoteCount());
        assertEquals(3L, result.get(0).getCommentCount());

        verify(ideaRepo, times(12)).countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(eq(userId), any(), any());
        verify(userActivityRepo, times(12)).countByUser_UserIdAndActivityTypeAndCreatedAtBetween(eq(userId), eq(Constants.ActivityType.VOTE), any(), any());
        verify(userActivityRepo, times(12)).countByUser_UserIdAndActivityTypeAndCreatedAtBetween(eq(userId), eq(Constants.ActivityType.COMMENT), any(), any());
    }

    @Test
    void testGetMonthlyEngagement_EmptyYear() {
        Integer userId = 1;
        int year = 2024;
        
        when(ideaRepo.countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(anyInt(), any(), any())).thenReturn(0L);
        when(userActivityRepo.countByUser_UserIdAndActivityTypeAndCreatedAtBetween(anyInt(), any(), any(), any())).thenReturn(0L);

        List<MonthlyEngagementDTO> result = analyticService.getMonthlyEngagement(userId, year);

        assertEquals(12, result.size());
        assertEquals(0L, result.get(5).getIdeaCount()); // Check June
        verify(ideaRepo, times(12)).countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(anyInt(), any(), any());
    }
    
    
    
    @Test
    void testGetPerformanceEmployee_Success() {
        // Arrange
        Integer userId = 1;
        long mockedIdeaCount = 3L;

        // Stub the repository to return a fixed count for any month call
        when(ideaRepo.countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(
                eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockedIdeaCount);

        // Act
        List<EmployeePerformanceDTO> result = analyticService.getPerfomanceEmployee(userId);

        // Assert
        assertNotNull(result);
        assertEquals(12, result.size()); // Verify 12 months are processed
        
        // Verify a specific month's data structure
        EmployeePerformanceDTO januaryData = result.get(0);
        assertEquals("JANUARY", januaryData.getMonth());
        assertEquals(mockedIdeaCount, januaryData.getCount());

        // Verify the repository was called exactly 12 times (once per month)
        verify(ideaRepo, times(12)).countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(
                eq(userId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetPerformanceEmployee_ZeroIdeas() {
        Integer userId = 5;
        
        when(ideaRepo.countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(anyInt(), any(), any())).thenReturn(0L);

        List<EmployeePerformanceDTO> result = analyticService.getPerfomanceEmployee(userId);

        assertEquals(12, result.size());
        assertEquals("DECEMBER", result.get(11).getMonth());
        assertEquals(0L, result.get(11).getCount());
        
        verify(ideaRepo, times(12)).countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(anyInt(), any(), any());
    }
    
    
    
    @Test
    void testGetIdeaDistribution_Success() {
        when(ideaRepo.findCategoryCountsForUser(1)).thenReturn(Collections.emptyList());
        analyticService.getIdeaDistribution(1);
        verify(ideaRepo).findCategoryCountsForUser(1);
    }

    
    
    
    // -------------------------------------------------------------------------
    // 3. Reviewer Logic
    // -------------------------------------------------------------------------

    
    
    
    @Test
    void testGetPerformanceReviewer_Success() {
        List<ReviewerPerformanceDTO> result = analyticService.getPerfomanceReviewer(1, 2023);
        assertEquals(12, result.size());
        verify(assignedReviewerRepo, atLeast(12)).countOnTimeByUserAndMonthWithin3Days(any(), any(), any());
    }

    @Test
    void testGetReviewerAcceptanceCount_Success() {
        when(userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(any(), any(), any(), any(), any())).thenReturn(5L);
        
        List<AcceptApproveCountDTO> result = analyticService.getReviewerAcceptanceCount(1, 2023);
        assertEquals(5, result.get(0).getCount());
    }
    
    
    
    @Test
    void testGetReviewerDecisionBreakdown_Success() {
        Integer userId = 1;
        int year = 2023;

        when(userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(
                eq(userId), eq(Constants.ActivityType.REVIEWDISCUSSION), eq(Constants.IdeaStatus.ACCEPTED), any(), any()))
            .thenReturn(10L);
        
        when(userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(
                eq(userId), eq(Constants.ActivityType.REVIEWDISCUSSION), eq(Constants.IdeaStatus.REJECTED), any(), any()))
            .thenReturn(5L);
            
        when(userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(
                eq(userId), eq(Constants.ActivityType.REVIEWDISCUSSION), eq(Constants.IdeaStatus.REFINE), any(), any()))
            .thenReturn(2L);

        List<DecisionBreakdownDTO> result = analyticService.getReviewerDecisionBreakdown(userId, year);

        assertNotNull(result);
        assertEquals(12, result.size());
        
        DecisionBreakdownDTO march = result.get(2); // Index 2 is March
        assertEquals("MARCH", march.getMonth());
        assertEquals(10L, march.getAcceptedCount());
        assertEquals(5L, march.getRejectedCount());
        assertEquals(2L, march.getReassignCount());

        verify(userActivityRepo, times(36)).countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(
                eq(userId), eq(Constants.ActivityType.REVIEWDISCUSSION), any(), any(), any());
    }

    @Test
    void testGetReviewerDecisionBreakdown_AllZeros() {
        Integer userId = 2;
        int year = 2024;
        
        when(userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(
                anyInt(), any(), any(), any(), any()))
            .thenReturn(0L);

        List<DecisionBreakdownDTO> result = analyticService.getReviewerDecisionBreakdown(userId, year);

        assertEquals(12, result.size());
        assertEquals(0L, result.get(0).getAcceptedCount());
        verify(userActivityRepo, times(36)).countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(
                anyInt(), any(), any(), any(), any());
    }
    
    

    // -------------------------------------------------------------------------
    // 4. Admin Logic
    // -------------------------------------------------------------------------

    
    @Test
    void testGetProjectApprovalMetrics_Success() {
        int year = 2023;
        long mockedAcceptedCount = 15L;
        long mockedApprovedCount = 10L;

        when(ideaRepo.countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(
                eq(Constants.IdeaStatus.ACCEPTED), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockedAcceptedCount);

        when(ideaRepo.countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(
                eq(Constants.IdeaStatus.APPROVED), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockedApprovedCount);

        List<ProjectApprovalMetricsDTO> result = analyticService.getProjectApprovalMetrics(year);

        assertNotNull(result);
        assertEquals(12, result.size());

        ProjectApprovalMetricsDTO juneMetrics = result.get(5);
        assertEquals("JUNE", juneMetrics.getMonth());
        assertEquals(mockedAcceptedCount, juneMetrics.getTotalAcceptedIdeaCount());
        assertEquals(mockedApprovedCount, juneMetrics.getTotalApprovedIdeaCount());

        verify(ideaRepo, times(24)).countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(
                any(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetProjectApprovalMetrics_EmptyYear() {
        int year = 2024;
        when(ideaRepo.countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(any(), any(), any()))
            .thenReturn(0L);

        List<ProjectApprovalMetricsDTO> result = analyticService.getProjectApprovalMetrics(year);

        assertEquals(12, result.size());
        assertEquals(0L, result.get(0).getTotalAcceptedIdeaCount());
        assertEquals(0L, result.get(0).getTotalApprovedIdeaCount());
        
        verify(ideaRepo, times(24)).countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(any(), any(), any());
    }
    
    
    
    @Test
    void testGetApprovedIdeaDistribution_Success() {
        // Arrange
        int year = 2023;
        CategoryCountDTO techCount = new CategoryCountDTO(1,"Tech", 15L);
        CategoryCountDTO healthCount = new CategoryCountDTO(2,"Health", 10L);
        List<CategoryCountDTO> mockedDistribution = List.of(techCount, healthCount);

        when(ideaRepo.findTotalCategoryCountCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockedDistribution);

        List<CategoryCountDTO> result = analyticService.getApprovedIdeaDistribution(year);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Tech", result.get(0).getCategoryName());
        assertEquals(15L, result.get(0).getIdeaCount());

        verify(ideaRepo, times(1)).findTotalCategoryCountCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetApprovedIdeaDistribution_EmptyResult() {
        int year = 2024;
        when(ideaRepo.findTotalCategoryCountCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        List<CategoryCountDTO> result = analyticService.getApprovedIdeaDistribution(year);

        assertTrue(result.isEmpty());
        verify(ideaRepo).findTotalCategoryCountCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    
    
    
    @Test
    void testGetDepartmentStatistics_Success() {
        // Arrange
        Department d = new Department();
        d.setDeptId(1); 
        d.setDeptName("IT");
        d.setDeleted(false); // Important: must be false to be returned by findAllByDeletedFalse
        
        when(departmentRepo.findAllByDeletedFalse()).thenReturn(List.of(d));
        when(ideaRepo.countByCategory_Department_DeptIdAndCreatedAtBetween(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(5L);
        when(userActivityRepo.countByUser_Department_DeptIdAndActivityTypeAndCreatedAtBetween(
                eq(1), eq(Constants.ActivityType.VOTE), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(3L);
        when(userActivityRepo.countByUser_Department_DeptIdAndActivityTypeAndCreatedAtBetween(
                eq(1), eq(Constants.ActivityType.COMMENT), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(2L);

        // Act
        List<DepartmentMetricsDTO> result = analyticService.getDepartmentStatistics(2023, null);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals("IT", result.get(0).getDeptName());
        assertEquals(5L, result.get(0).getIdeaCount());
        assertEquals(3L, result.get(0).getVoteCount());
        assertEquals(2L, result.get(0).getCommentCount());
        
        verify(departmentRepo).findAllByDeletedFalse();
        verify(ideaRepo).countByCategory_Department_DeptIdAndCreatedAtBetween(eq(1), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(userActivityRepo, times(2)).countByUser_Department_DeptIdAndActivityTypeAndCreatedAtBetween(
                eq(1), any(Constants.ActivityType.class), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    
    
    
    // -------------------------------------------------------------------------
    // 5. Leaderboard Logic
    // -------------------------------------------------------------------------

    
    
    
    @Test
    void testGetLeaderboard_Success_UserNotInTopTen() {
        // Mocking top 10 as empty or other users
        when(userRepo.findTopNUser(any(PageRequest.class))).thenReturn(Collections.emptyList());
        when(userRepo.findById(1)).thenReturn(Optional.of(sampleUser));
        when(userRepo.countByXpGreaterThan(500)).thenReturn(15);

        List<LeaderboardDTO> result = analyticService.getLeaderboard(1);

        assertEquals(1, result.size());
        assertEquals(16, result.get(0).getRank()); // 15 users higher + 1
    }

    @Test
    void testGetLeaderboard_Failure_UserNotFound() {
        when(userRepo.findTopNUser(any(PageRequest.class))).thenReturn(Collections.emptyList());
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> analyticService.getLeaderboard(999));
    }
}