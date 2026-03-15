// Analytics API – typed to match Spring Boot AnalyticsController DTOs
import restApi from "./restApi";

// ─── Request / Response Types ────────────────────────────────────

export interface IGenerateReport {
  scope: string;
  scopeId: number;
  userId: number;
  year: string;
  month: string;
}

export interface IReport {
  id: number;
  scope: string;
  dataOf: string;
  ideasSubmitted: number;
  approvedCount: number;
  participationCount: number;
  userName: string;
  createdAt: string;
  updatedAt: string;
}

export interface IViewCategory {
  categoryId: number;
  name: string;
}

// ─── Analytics DTO Types (mirror Java DTOs) ──────────────────────

export interface YearlyEngagementDTO {
  year: number;
  ideaCount: number;
  voteCount: number;
  commentCount: number;
}

export interface MonthlyEngagementDTO {
  month: string;
  ideaCount: number;
  voteCount: number;
  commentCount: number;
}

export interface EmployeePerformanceDTO {
  month: string;
  count: number;
}

export interface CategoryCountDTO {
  categoryId: number;
  categoryName: string;
  ideaCount: number;
}

export interface ReviewerPerformanceDTO {
  month: string;
  assignedIdeaCount: number;
  reviewedOnTimeCount: number;
}

export interface AcceptApproveCountDTO {
  month: string;
  count: number;
}

export interface DecisionBreakdownDTO {
  month: string;
  acceptedCount: number;
  rejectedCount: number;
  reassignCount: number;
}

export interface ProjectApprovalMetricsDTO {
  month: string;
  totalAcceptedIdeaCount: number;
  totalApprovedIdeaCount: number;
}

export interface DepartmentMetricsDTO {
  deptName: string;
  ideaCount: number;
  voteCount: number;
  commentCount: number;
}

export interface LeaderboardDTO {
  userId: number;
  userName: string;
  role: string;
  xp: number;
  rank: number;
}

// ─── Utility fetchers used by Report page ────────────────────────

export const getAllDept = () => restApi.get("/api/profile/departmentID");
export const getAllCategories = () => restApi.get("/api/categories");
export const getProfile = () => restApi.get("/api/profile/me");

// ─── Analytics Endpoints ─────────────────────────────────────────

/** Returns total idea count for a given year (Long). */
export const getTotalIdeasSubmitted = (year: number) =>
  restApi.get<number>(`/analytics/totalIdeasSubmitted/${year}`);

/** Year-wise engagement for userId between startYear and endYear. */
export const getYearWiseEngagement = (userId: number, startYear: number, endYear: number) =>
  restApi.get<YearlyEngagementDTO[]>("/analytics/getYearWiseEngagement", {
    params: { userId, startYear, endYear },
  });

/** Month-wise engagement for userId in a given year. */
export const getMonthWiseEngagement = (userId: number, year: number) =>
  restApi.get<MonthlyEngagementDTO[]>("/analytics/getMonthWiseEngagement", {
    params: { userId, year },
  });

/** Employee performance: ideas submitted month-wise (all years). */
export const getPerformanceEmployee = (userId: number) =>
  restApi.get<EmployeePerformanceDTO[]>(`/analytics/getPerformanceEmployee/${userId}`);

/** Idea category distribution for a user. */
export const getIdeaDistribution = (userId: number) =>
  restApi.get<CategoryCountDTO[]>(`/analytics/getIdeaDistribution/${userId}`);

/** Reviewer performance: assigned vs reviewed-on-time, monthly for a year. */
export const getPerformanceReviewer = (userId: number, year: number) =>
  restApi.get<ReviewerPerformanceDTO[]>(`/analytics/getPerformanceReviewer/${userId}`, {
    params: { year },
  });

/** Reviewer acceptance count monthly for a year. */
export const getReviewerAcceptanceCount = (userId: number, year: number) =>
  restApi.get<AcceptApproveCountDTO[]>(`/analytics/getReviewerAcceptanceCount/${userId}`, {
    params: { year },
  });

/** Reviewer decision breakdown (accepted/rejected/reassigned) monthly for a year. */
export const getReviewerDecisionBreakdown = (userId: number, year: number) =>
  restApi.get<DecisionBreakdownDTO[]>(`/analytics/getReviewerDecisionBreakdown/${userId}`, {
    params: { year },
  });

/** Admin: total accepted + approved ideas monthly for a year. */
export const getProjectApprovalMetrics = (year: number) =>
  restApi.get<ProjectApprovalMetricsDTO[]>("/analytics/getProjectApprovalMetrics", {
    params: { year },
  });

/** Admin: category distribution of approved ideas for a year. */
export const getApprovedIdeaDistribution = (year: number) =>
  restApi.get<CategoryCountDTO[]>("/analytics/getApprovedIdeaDistribution", {
    params: { year },
  });

/** Admin: department-wise participation metrics for a year+month. */
export const getDepartmentStatistics = (year: number, month: number) =>
  restApi.get<DepartmentMetricsDTO[]>("/analytics/getDepartmentStatistics", {
    params: { year, month },
  });

/** Leaderboard for top contributors; userId used to find the current user's rank. */
export const getLeaderboard = (userId: number) =>
  restApi.get<LeaderboardDTO[]>(`/analytics/getLeaderboard/${userId}`);

// ─── Report Endpoints ─────────────────────────────────────────────

export const getReportList = (year: number, month: number) =>
  restApi.get<IReport[]>("/analytics/getReportList", { params: { year, month } });

export const getReportById = (reportId: number) =>
  restApi.get<IReport>(`/analytics/getReportById/${reportId}`);

export const generateReport = (reportObj: IGenerateReport) =>
  restApi.post<string>("/analytics/generateReport", reportObj);

export const deleteReport = (reportId: number) =>
  restApi.delete<string>(`/analytics/deleteReportById/${reportId}`);
