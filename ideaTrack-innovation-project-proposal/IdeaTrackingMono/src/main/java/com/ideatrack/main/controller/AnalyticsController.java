package com.ideatrack.main.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ideatrack.main.dto.analytics.AcceptApproveCountDTO;
import com.ideatrack.main.dto.analytics.CategoryCountDTO;
import com.ideatrack.main.dto.analytics.DecisionBreakdownDTO;
import com.ideatrack.main.dto.analytics.DepartmentMetricsDTO;
import com.ideatrack.main.dto.analytics.MonthlyEngagementDTO;
import com.ideatrack.main.dto.analytics.ProjectApprovalMetricsDTO;
import com.ideatrack.main.dto.analytics.ReportDTO;
import com.ideatrack.main.dto.analytics.ReviewerPerformanceDTO;
import com.ideatrack.main.dto.analytics.EmployeePerformanceDTO;
import com.ideatrack.main.dto.analytics.GenerateReportDTO;
import com.ideatrack.main.dto.analytics.LeaderboardDTO;
import com.ideatrack.main.dto.analytics.YearlyEngagementDTO;
import com.ideatrack.main.service.AnalyticService;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

	private final AnalyticService servObj;

	public AnalyticsController(AnalyticService servObj) {
		this.servObj = servObj;
	}
	
	
//	API Test Function
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN','EMPLOYEE', 'REVIEWER')")
	@GetMapping("/test")
	public ResponseEntity<String> test() {
		String returnStr = "Test API Endpoint called";
	
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(returnStr);
	}
	
	
	
	
//	Returns the value of total ideas submitted for the given year.	
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN','EMPLOYEE', 'REVIEWER')")
	@GetMapping("/totalIdeasSubmitted/{year}")
	public ResponseEntity<Long> totalIdeasSubmitted(@PathVariable int year){
		
		Long totalIdeaCount = servObj.getTotalIdeaCount(year);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(totalIdeaCount);
	}
	
	
	
//	************************************* Report Logic **************************************************

	
	

//	Gives month-wise report generated for the given year.	
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
	@GetMapping("/getReportList")
	public ResponseEntity <List<ReportDTO>> getReportList(@RequestParam int year, @RequestParam int month){
		
		List<ReportDTO> reportList = servObj.getReportList(year, month);
				
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(reportList);		
	}
	
	
	
	
	
//	Gives report details for the given reportId.	
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
	@GetMapping("/getReportById/{reportId}")
	public ResponseEntity<ReportDTO> getReportById(@PathVariable Integer reportId){
		
		ReportDTO reportById = servObj.getReportById(reportId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(reportById);
	}
	
	
	
	
//	Create Report for a given Scope(DEPARTMENT, CATEGORY, PERIOD)
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
	@PostMapping("/generateReport")
	public ResponseEntity<String> generateReport(@RequestBody GenerateReportDTO request){
		
		Integer reportId = servObj.generateReport(request);
		
		String returnStr = "Report created successfully with report ID: " + reportId; 
		
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(returnStr);
	}
	
	
	
	
//	Deletes report for the given reportId.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
	@DeleteMapping("/deleteReportById/{reportId}")
	public ResponseEntity<String> deleteReportById(@PathVariable Integer reportId){

		servObj.deleteReportById(reportId);
		
		String returnStr = "Report deleted successfully with report ID: " + reportId; 
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(returnStr);
	}
	

	
	
	

//	************************************** Employee Logic *************************************************
	
	
	
	
	
//	Returns the Year-wise Engagement Statistics (vote count, idea count, comments count) of the Employee for given start and end year.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN','EMPLOYEE', 'REVIEWER')")
	@GetMapping("/getYearWiseEngagement")
	public ResponseEntity<List<YearlyEngagementDTO>> getYearWiseEngagement(@RequestParam Integer userId, @RequestParam int startYear, @RequestParam int endYear){
		
		List<YearlyEngagementDTO> engagementList = servObj.getYearlyEngagement(userId, startYear, endYear);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(engagementList);
	}
	
	
	
	
//	Returns the Month-wise Engagement Statistics (vote count, idea count, comments count) of the Employee for a given year.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN','EMPLOYEE', 'REVIEWER')")
	@GetMapping("/getMonthWiseEngagement")
	public ResponseEntity<List<MonthlyEngagementDTO>> getMonthWiseEngagement(@RequestParam Integer userId, @RequestParam int year){
		
		List<MonthlyEngagementDTO> engagementList = servObj.getMonthlyEngagement(userId, year);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(engagementList);
	}
	
	
	

	
	
//	Returns the Performance analytics of employee by number of ideas submitted (month-wise).
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN','EMPLOYEE', 'REVIEWER')")
	@GetMapping("/getPerformanceEmployee/{userId}")
	public ResponseEntity<List<EmployeePerformanceDTO>> getPerformanceEmployee(@PathVariable Integer userId){
		
		List<EmployeePerformanceDTO> monthlyStats = servObj.getPerfomanceEmployee(userId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(monthlyStats);
	}
	
	
	
	
	
	
//	Idea distribution of submitted ideas into different categories.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN','EMPLOYEE', 'REVIEWER')")
	@GetMapping("/getIdeaDistribution/{userId}")
	public ResponseEntity<List<CategoryCountDTO>> getIdeaDistribution(@PathVariable Integer userId){
		
		List<CategoryCountDTO> ideaDistribution = servObj.getIdeaDistribution(userId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(ideaDistribution);		
	}
	
	
	
	
	
//************************************* Reviewer Endpoints **************************************************
	
	
	
	
	
	
//	Reviewer Performance: Ideas assigned to review monthly and ideas reviewed on time for a given year.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN', 'REVIEWER')")
	@GetMapping("/getPerformanceReviewer/{userId}")
	public ResponseEntity<List<ReviewerPerformanceDTO>> getPerformanceReviewer (@PathVariable Integer userId, @RequestParam int year){
		
		List<ReviewerPerformanceDTO> monthlyStat = servObj.getPerfomanceReviewer(userId, year);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(monthlyStat);
	}
	
	
	
	
//	Reviewer Acceptance Count (No. of ideas accepted) month-wise for a given year.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN', 'REVIEWER')")
	@GetMapping("/getReviewerAcceptanceCount/{userId}")
	public ResponseEntity<List<AcceptApproveCountDTO>> getReviewerAcceptanceCount(@PathVariable Integer userId, @RequestParam int year){
		
		List<AcceptApproveCountDTO> monthlyRate = servObj.getReviewerAcceptanceCount(userId, year);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(monthlyRate);		
	}
	
	
	
	
//	Reviewer Decision Breakdown(Accepted, Rejected, Reassign) month-wise for a given year.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN', 'REVIEWER')")
	@GetMapping("/getReviewerDecisionBreakdown/{userId}")
	public ResponseEntity<List<DecisionBreakdownDTO>> getReviewerDecisionBreakdown(@PathVariable Integer userId, @RequestParam int year){
		
		List<DecisionBreakdownDTO> reviewerDecisionBreakdown = servObj.getReviewerDecisionBreakdown(userId, year);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(reviewerDecisionBreakdown);
	}
	
	
	
	
	
	
//	************************************** Admin Endpoints ************************************************
	
	
	
	
	
//	Total ideas and approved ideas month-wise for the given year.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
	@GetMapping("/getProjectApprovalMetrics")
	public ResponseEntity<List<ProjectApprovalMetricsDTO>> getProjectApprovalMetrics(@RequestParam int year){
		
		List<ProjectApprovalMetricsDTO> approvalMetrics = servObj.getProjectApprovalMetrics(year);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(approvalMetrics);
		
	}
	
	
	
	
//	Total Category Distribution of all the approved Ideas for the given year.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
	@GetMapping("/getApprovedIdeaDistribution")
	public ResponseEntity<List<CategoryCountDTO>> getApprovedIdeaDistribution(@RequestParam int year){
		
		List<CategoryCountDTO> categoryDistribution = servObj.getApprovedIdeaDistribution(year);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(categoryDistribution);
	}
	
	
	
	
//	Department-wise participation/engagement metrics(ideas, vote, comments)
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
	@GetMapping("/getDepartmentStatistics")
	public ResponseEntity<List<DepartmentMetricsDTO>> getDepartmentStatistics(@RequestParam Integer year, @RequestParam Integer month){
		
		List<DepartmentMetricsDTO> departmentMetrics = servObj.getDepartmentStatistics(year, month);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(departmentMetrics);
	}
	
	
	
	
//	************************************* Leaderboard Endpoint ***********************************************

	
	
	
//	Leaderboard showing the Top contributor based on the XP achieved along with the user Position.
	@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN','EMPLOYEE', 'REVIEWER')")
	@GetMapping("/getLeaderboard/{userId}")
	public ResponseEntity<List<LeaderboardDTO>> getLeaderboard (@PathVariable Integer userId){
		
		List<LeaderboardDTO> leaderboardList = servObj.getLeaderboard(userId);
		
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(leaderboardList);
		
	}
	
	
}
