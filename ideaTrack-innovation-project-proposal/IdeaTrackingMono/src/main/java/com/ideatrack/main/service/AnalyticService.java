package com.ideatrack.main.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.Report;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.analytics.MonthlyEngagementDTO;
import com.ideatrack.main.dto.analytics.ProjectApprovalMetricsDTO;
import com.ideatrack.main.dto.analytics.ReportDTO;
import com.ideatrack.main.dto.analytics.ReviewerPerformanceDTO;
import com.ideatrack.main.dto.analytics.AcceptApproveCountDTO;
import com.ideatrack.main.dto.analytics.CategoryCountDTO;
import com.ideatrack.main.dto.analytics.DecisionBreakdownDTO;
import com.ideatrack.main.dto.analytics.DepartmentMetricsDTO;
import com.ideatrack.main.dto.analytics.EmployeePerformanceDTO;
import com.ideatrack.main.dto.analytics.GenerateReportDTO;
import com.ideatrack.main.dto.analytics.LeaderboardDTO;
import com.ideatrack.main.dto.analytics.YearlyEngagementDTO;
import com.ideatrack.main.exception.*; // Integrated custom exceptions
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.ICategoryRepository;
import com.ideatrack.main.repository.IDepartmentRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IReportRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class AnalyticService {

	private final IReportRepository reportRepo;
	private final IIdeaRepository ideaRepo;
	private final IUserRepository userRepo;
	private final IUserActivityRepository userActivityRepo;
	private final IAssignedReviewerToIdeaRepository assignedReviewerRepo;
	private final IDepartmentRepository departmentRepo;
	private final ICategoryRepository categoryRepo;
	private final ModelMapper modelMapper;

	public AnalyticService(IReportRepository reportRepo, IIdeaRepository ideaRepo, 
	                      IUserRepository userRepo, IUserActivityRepository userActivityRepo,
	                      IAssignedReviewerToIdeaRepository assignedReviewerRepo, 
	                      IDepartmentRepository departmentRepo, ICategoryRepository categoryRepo,
	                      ModelMapper modelMapper) {
		this.reportRepo = reportRepo;
		this.ideaRepo = ideaRepo;
		this.userRepo = userRepo;
		this.userActivityRepo = userActivityRepo;
		this.assignedReviewerRepo = assignedReviewerRepo;
		this.departmentRepo = departmentRepo;
		this.categoryRepo = categoryRepo;
		this.modelMapper = modelMapper;
	}	
	
	
	
	
	
	
//	Returns the total number of ideas submitted for the given year.
	public long getTotalIdeaCount(int year) {
		
		Year y = Year.of(year);
		LocalDateTime start = y.atDay(1).atStartOfDay();
		LocalDateTime end = y.plusYears(1).atDay(1).atStartOfDay();
		
		long totalIdeasCount = ideaRepo.countByDeletedFalseAndCreatedAtBetween(start, end);
		
		log.info("Total Submitted Ideas: " + totalIdeasCount);
		
		return totalIdeasCount;
	}
	
	
		
	
	
	
//	************************************* Report Logic **************************************************
	
	
	
//	Gives month-wise report generated for the given year.
	public List<ReportDTO> getReportList(int year, int month) {
		
		LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
		LocalDateTime end = LocalDate.of(year, month + 1, 1).atStartOfDay();
		
		List<Report> reportList= reportRepo.findAllByDeletedFalseAndCreatedAtBetween(start, end);

		List<ReportDTO> reportDTOList = reportList.stream()
										.map(report -> {
											ReportDTO reportObj = modelMapper.map(report, ReportDTO.class);
											
											if (report.getUser() != null) {
									            reportObj.setUserName(report.getUser().getName());
									        }
											return reportObj;
										})
										.toList();
		log.info("Report List: " + reportDTOList);
		
		return reportDTOList;
	}
	
	
	
//	Gives report details for the given reportId.
	public ReportDTO getReportById (Integer reportId) {
		
		Report reportById = reportRepo.findById(reportId)
				.orElseThrow(() -> new ReportNotFoundException("Report not found for ID: " + reportId));
		
		ReportDTO reportDTOById = modelMapper.map(reportById,ReportDTO.class);
		if (reportById.getUser() != null) {
		    reportDTOById.setUserName(reportById.getUser().getName());
		}
		
		log.info("Report for the given ID: "+reportId+" ; Data: "+reportDTOById);
		
		return reportDTOById;
		
	}
	
	
	
	
//	Create Report for a given Scope(DEPARTMENT, CATEGORY, PERIOD)
	public Integer generateReport(GenerateReportDTO request) {
		if (request.getScope() == null) {
            throw new IllegalArgumentException("Scope must not be null");
        }
		
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        
        User user = userRepo.findById(request.getUserId())
        			.orElseThrow(() -> new UserNotFoundException("Invalid User userID: " + request.getUserId()));
        
        int year = (request.getYear() != null) ? request.getYear() : LocalDateTime.now().getYear();
        
        LocalDateTime start;
        LocalDateTime end;
//      Monthly Logic for scope = Period.
        if (request.getScope() == Constants.Scope.PERIOD && request.getMonth() != null) {

            start = LocalDateTime.of(year, request.getMonth(), 1, 0, 0);
            end = start.plusMonths(1);
        }
//      Yearly Logic for scope = Department or Category.
        else {
            start = LocalDateTime.of(year, 1, 1, 0, 0);
            end = start.plusYears(1);
        }
        long ideasSubmitted;
        long approvedCount;
        long participationCount;   
        String dataOf;
        switch (request.getScope()) {

	        case DEPARTMENT:
	        	Integer deptId = request.getScopeId();
	        	dataOf = departmentRepo.findById(deptId)
	        			.orElseThrow(() -> new DepartmentNotFound("Department not found ID: " + deptId)).getDeptName();
	            ideasSubmitted = ideaRepo.countByCategory_Department_DeptIdAndCreatedAtBetween(deptId, start, end);
	            approvedCount = ideaRepo.countByIdeaStatusAndCategory_Department_DeptIdAndDeletedFalseAndCreatedAtBetween(Constants.IdeaStatus.APPROVED, deptId, start, end);
	            participationCount = ideaRepo.countDistinctUsersByDepartment(deptId, start, end);
	            break;
	
	        case CATEGORY:
	        	Integer catId = request.getScopeId();
	        	dataOf = categoryRepo.findById(catId)
	        			.orElseThrow(() -> new CategoryNotFound("Category not found ID: " + catId)).getName();
	        	ideasSubmitted = ideaRepo.countByCategory_CategoryIdAndCreatedAtBetween(catId, start, end);
	            approvedCount = ideaRepo.countByIdeaStatusAndCategory_CategoryIdAndDeletedFalseAndCreatedAtBetween(Constants.IdeaStatus.APPROVED, catId, start, end);
	            participationCount = ideaRepo.countDistinctUsersByCategory(catId, start, end);
	            break;
	
	        case PERIOD:
	        	if (request.getMonth() != null) { 
	                String monthName = java.time.Month.of(request.getMonth()).name();	//Converted
	                dataOf = monthName + " " + year;
	            } else {
	                dataOf = "Year " + year;
	            }
	        	ideasSubmitted = ideaRepo.countByDeletedFalseAndCreatedAtBetween(start, end);
	            approvedCount = ideaRepo.countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(Constants.IdeaStatus.APPROVED, start, end);
	            participationCount = ideaRepo.countDistinctUsersByPeriod(start, end);
	            break;
	        
	        default: throw new IllegalArgumentException("Unsupported scope: " + request.getScope());
	    }
        
        Report report = new Report();
        report.setScope(request.getScope());
        report.setUser(user);
        report.setIdeasSubmitted(ideasSubmitted);
        report.setApprovedCount(approvedCount);
        report.setParticipationCount(participationCount);
        report.setDataOf(dataOf);
        Report saved = reportRepo.save(report);
        Integer reportId = saved.getId();
        
		log.info("Report generated with Report ID: " + reportId);
		return reportId;
	}
	

	
//	Deletes report for the given reportId.
	public void deleteReportById(Integer reportId) {
		
		Report reportObj = reportRepo.findById(reportId)
				.orElseThrow(() -> new ReportNotFoundException("Report not found for ID: " + reportId));
		
		reportObj.setDeleted(true);
		
		log.info("Delete report: " + reportObj);
		
		reportRepo.save(reportObj);
		
	}
	
	
	
	
	
	
//	************************************** Employee Logic *************************************************
	
	
	
	
//	Returns the Year-wise Engagement Statistics (vote count, idea count, comments count) of the Employee for given start and end year.
	public List<YearlyEngagementDTO> getYearlyEngagement(Integer userId, int startYear, int endYear){
		
		List<YearlyEngagementDTO> engagementList = new ArrayList<>();	

		LocalDateTime start = null;
		LocalDateTime end = null;
		
		for(int year = startYear; year <= endYear; year++)
		{
			Year y = Year.of(year);
			start = y.atDay(1).atStartOfDay();
			end = y.plusYears(1).atDay(1).atStartOfDay();
			
			long userIdeaCount = ideaRepo.countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(userId, start, end);
			
			long userVoteCount = userActivityRepo.countByUser_UserIdAndActivityTypeAndCreatedAtBetween(userId, Constants.ActivityType.VOTE, start, end);
			long userCommentCount = userActivityRepo.countByUser_UserIdAndActivityTypeAndCreatedAtBetween(userId, Constants.ActivityType.COMMENT, start, end);
			
			
			engagementList.add(new YearlyEngagementDTO(year, userIdeaCount, userVoteCount, userCommentCount));			
		}
		
		log.info("Year-wise Engagement: " + engagementList);
		
		return engagementList;
	}
	
	
	
	
//	Returns the Month-wise Engagement Statistics (vote count, idea count, comments count) of the Employee for a given year.
	public List<MonthlyEngagementDTO> getMonthlyEngagement(Integer userId, int year){
		
		List<MonthlyEngagementDTO> engagementList = new ArrayList<>();	

		LocalDateTime start = null;
		LocalDateTime end = null;

		Year currentYear = Year.of(year);
		
		for(int month = 1; month <= 12; month++)
		{
			YearMonth ym = YearMonth.of(currentYear.getValue(), month);
			start = ym.atDay(1).atStartOfDay();
			end = ym.plusMonths(1).atDay(1).atStartOfDay();
			
			long userIdeaCount = ideaRepo.countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(userId, start, end);
			
			long userVoteCount = userActivityRepo.countByUser_UserIdAndActivityTypeAndCreatedAtBetween(userId, Constants.ActivityType.VOTE, start, end);
			long userCommentCount = userActivityRepo.countByUser_UserIdAndActivityTypeAndCreatedAtBetween(userId, Constants.ActivityType.COMMENT, start, end);
			
			
			engagementList.add(new MonthlyEngagementDTO(ym.getMonth().name(), userIdeaCount, userVoteCount, userCommentCount));			
		}
		
		log.info("Month-wise Engagement: " + engagementList);
		
		return engagementList;
	}
	
		
	
	

//	Returns the Performance analytics of employee by number of ideas submitted (month-wise).
	public List<EmployeePerformanceDTO> getPerfomanceEmployee(Integer userId) {
	
		LocalDateTime start = null;
		LocalDateTime end = null;

		List<EmployeePerformanceDTO> monthlyStat = new ArrayList<>();
		
		Year currentYear = Year.now();
		
		for(int month = 1; month <= 12; month++)
		{
			YearMonth ym = YearMonth.of(currentYear.getValue(), month);
			start = ym.atDay(1).atStartOfDay();
			end = ym.plusMonths(1).atDay(1).atStartOfDay();
			
			long ideaCount = ideaRepo.countByUser_UserIdAndDeletedFalseAndCreatedAtBetween(userId, start, end);
			
			monthlyStat.add(new EmployeePerformanceDTO(ym.getMonth().name(), ideaCount));			
		}				
		
		log.info("Employee Monthly Stats: " + monthlyStat);
		
		return monthlyStat;
		
	}
	
	
	
	
	
//	Idea distribution of submitted ideas into different categories.
	public List<CategoryCountDTO> getIdeaDistribution(Integer userId){
		
		List<CategoryCountDTO> ideaDistro = ideaRepo.findCategoryCountsForUser(userId);

		log.info("Idea Distro: "+ideaDistro);
		
		return ideaDistro;
	}
	
	
	
	
	
	
//	**************************** Reviewer Logic *************************************************
	
	
	
	
//	Reviewer Performance: Ideas assigned to review monthly and ideas reviewed on time for a given year.
	public List<ReviewerPerformanceDTO> getPerfomanceReviewer(Integer userId, int year){
		
		List<ReviewerPerformanceDTO> reviewerPerformance = new ArrayList<>();
		
		LocalDateTime start = null;
		LocalDateTime end = null;		
		Year currentYear = Year.of(year);
		
		for(int month = 1; month <= 12; month++)
		{
			YearMonth ym = YearMonth.of(currentYear.getValue(), month);
			start = ym.atDay(1).atStartOfDay();
			end = ym.plusMonths(1).atDay(1).atStartOfDay();
						
			long assignedIdeaCount = assignedReviewerRepo.countByReviewer_UserIdAndDeletedFalseAndCreatedAtBetween(userId, start, end);
			long reviewedOnTimeCount = assignedReviewerRepo.countOnTimeByUserAndMonthWithin3Days(userId, start, end);
			
			reviewerPerformance.add(new ReviewerPerformanceDTO(ym.getMonth().name(),assignedIdeaCount, reviewedOnTimeCount));
		}
		
		log.info("Reviewer Performance: " + reviewerPerformance);
		
		return reviewerPerformance;
	}
	
	
	
	
//	Reviewer Acceptance Count (No. of ideas accepted) month-wise for a given year
	public List<AcceptApproveCountDTO> getReviewerAcceptanceCount(Integer userId, int year){

		List<AcceptApproveCountDTO> reviewerAcceptanceCount = new ArrayList<>();
		
		LocalDateTime start = null;
		LocalDateTime end = null;		
		Year currentYear = Year.of(year);
		
		for(int month = 1; month <= 12; month++)
		{
			YearMonth ym = YearMonth.of(currentYear.getValue(), month);
			start = ym.atDay(1).atStartOfDay();
			end = ym.plusMonths(1).atDay(1).atStartOfDay();
						
			long acceptedIdeaCount = userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(userId, Constants.ActivityType.REVIEWDISCUSSION,Constants.IdeaStatus.ACCEPTED,start, end);
			
			reviewerAcceptanceCount.add(new AcceptApproveCountDTO(ym.getMonth().name(), acceptedIdeaCount));
		}
		
		log.info("Reviewer Acceptance Count: " + reviewerAcceptanceCount);
		
		return reviewerAcceptanceCount;			
	}
	
	
	
//	Give Reviewer Decision Breakdown (Accpeted, Rejected and Reassign count)
	public List<DecisionBreakdownDTO> getReviewerDecisionBreakdown(Integer userId, int year){
		
		List<DecisionBreakdownDTO> reviewerDecisionBreakdown = new ArrayList<>();
		
		LocalDateTime start = null;
		LocalDateTime end = null;		
		Year currentYear = Year.of(year);
		
		for(int month = 1; month <= 12; month++)
		{
			YearMonth ym = YearMonth.of(currentYear.getValue(), month);
			start = ym.atDay(1).atStartOfDay();
			end = ym.plusMonths(1).atDay(1).atStartOfDay();
			
			long acceptedCount = userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(userId, Constants.ActivityType.REVIEWDISCUSSION,Constants.IdeaStatus.ACCEPTED,start, end);
			long rejectedCount = userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(userId, Constants.ActivityType.REVIEWDISCUSSION,Constants.IdeaStatus.REJECTED,start, end);
			long reassignCount = userActivityRepo.countByUser_UserIdAndActivityTypeAndDecisionAndDeletedFalseAndCreatedAtBetween(userId, Constants.ActivityType.REVIEWDISCUSSION,Constants.IdeaStatus.REFINE,start, end);
			
			reviewerDecisionBreakdown.add(new DecisionBreakdownDTO(ym.getMonth().name(), acceptedCount, rejectedCount, reassignCount));
		}		
		
		log.info("Reviewer Decision Breakdown: " + reviewerDecisionBreakdown);
		
		return reviewerDecisionBreakdown;
	}
	
	
	
	
	
//	************************************* Admin Logic *****************************************************
	
	
	
	
	
//	Total ideas and approved ideas month-wise for the given year.
	public List<ProjectApprovalMetricsDTO> getProjectApprovalMetrics(int year){
		
		List<ProjectApprovalMetricsDTO> approvalMetrics = new ArrayList<>();
		
		LocalDateTime start = null;
		LocalDateTime end = null;		
		Year currentYear = Year.of(year);
		
		for(int month = 1; month <= 12; month++)
		{
			YearMonth ym = YearMonth.of(currentYear.getValue(), month);
			start = ym.atDay(1).atStartOfDay();
			end = ym.plusMonths(1).atDay(1).atStartOfDay();
			
			long totalAcceptedIdeaCount = ideaRepo.countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(Constants.IdeaStatus.ACCEPTED,start, end);
			long totalApprovedIdeaCount = ideaRepo.countByIdeaStatusAndDeletedFalseAndCreatedAtBetween(Constants.IdeaStatus.APPROVED,start, end);
			
			approvalMetrics.add(new ProjectApprovalMetricsDTO(ym.getMonth().name(), totalAcceptedIdeaCount, totalApprovedIdeaCount));
		}	
		
		log.info("ApprovalMetrics: " + approvalMetrics);
		
		return approvalMetrics;		
	}
	
	
	
	
//	Total Category Distribution of all the approved Ideas for the given year.
	public List<CategoryCountDTO> getApprovedIdeaDistribution(int year){
		
		
		Year y = Year.of(year);
		LocalDateTime start = y.atDay(1).atStartOfDay();
		LocalDateTime end = y.plusYears(1).atDay(1).atStartOfDay();
		
		List<CategoryCountDTO> categoryDistribution = ideaRepo.findTotalCategoryCountCreatedBetween(start, end);

		log.info("Category Distribution: " + categoryDistribution);
		
		return categoryDistribution;
		
	}
	
	
// 	Department-wise participation/engagement metrics (ideas, votes, comments)
	public List<DepartmentMetricsDTO> getDepartmentStatistics(Integer year, Integer month) {
	    
	    List<DepartmentMetricsDTO> departmentMetricsList = new ArrayList<>();
	    LocalDateTime start;
	    LocalDateTime end;
	    
	    if (month != null && month > 0) {
	        start = LocalDateTime.of(year, month, 1, 0, 0);
	        end = start.plusMonths(1);
	    } else {
	        start = LocalDateTime.of(year, 1, 1, 0, 0);
	        end = start.plusYears(1);
	    }
	    
	    List<Department> departments = departmentRepo.findAllByDeletedFalse();
	    
	    for (Department dept : departments) {
	        Integer deptId = dept.getDeptId();
	        
	        // 1. Idea Count (Ideas -> Category -> Department)
	        long ideaCount = ideaRepo.countByCategory_Department_DeptIdAndCreatedAtBetween(deptId, start, end);
	        
	        // 2. Vote Count (Activity -> User -> Department)
	        long voteCount = userActivityRepo.countByUser_Department_DeptIdAndActivityTypeAndCreatedAtBetween(deptId, Constants.ActivityType.VOTE, start, end);
	            
	        // 3. Comment Count (Activity -> User -> Department)
	        long commentCount = userActivityRepo.countByUser_Department_DeptIdAndActivityTypeAndCreatedAtBetween(deptId, Constants.ActivityType.COMMENT, start, end);
	            
	        departmentMetricsList.add(new DepartmentMetricsDTO(dept.getDeptName(), ideaCount,voteCount, commentCount));
	    }
	    
	    log.info("Fetched Department Metrics for {}-{}: {}", year, month, departmentMetricsList);
	    return departmentMetricsList;
	}

	
	
	
	
//	************************************* Leaderboard Logic ***********************************************
	
	
	
//	Leaderboard showing the Top contributor based on the XP achieved along with the user Position.
	public List<LeaderboardDTO> getLeaderboard(Integer userId){
		
		int topNumber = 10; // Number of top contributors to display.
		
		List<LeaderboardDTO> leaderboardList = new ArrayList<>();
		
		List<User> topUsers = userRepo.findTopNUser(PageRequest.of(0, topNumber));
		
		
		
		boolean isUserInTopTen = false;
	    int rankCounter = 1;
		
	    for (User user : topUsers)
	    {
	    	leaderboardList.add( new LeaderboardDTO(user.getUserId(), user.getName(), user.getRole(), user.getTotalXP(), rankCounter));
	    	rankCounter++;
	    	
	        if (user.getUserId().equals(userId))
	        {
	            isUserInTopTen = true;
	        }
	    }		
	    
	    if (!isUserInTopTen)
	    {
	    	// Exception implementation: Replaced .get() with UserNotFound
	    	User currentUser = userRepo.findById(userId)
	    			.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
	    	int currentUserRank = userRepo.countByXpGreaterThan(currentUser.getTotalXP());
	    	int actualRank = currentUserRank + 1;
	    	
	    	leaderboardList.add(new LeaderboardDTO(currentUser.getUserId(), currentUser.getName(), currentUser.getRole(), currentUser.getTotalXP(), actualRank));
	    }
	    
		log.info("Leaderboard: " + leaderboardList);
		
		return leaderboardList;
		
	}

	


	
}