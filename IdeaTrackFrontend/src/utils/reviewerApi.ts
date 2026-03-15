// src/utils/reviewerApi.ts
// Reviewer module API — uses shared restApi (axios, jwt-token auth)
import restApi from "./restApi";

// ─── Types ────────────────────────────────────────────────────────────────────

export interface ReviewerDashboardDTO {
  ideaId: number;
  ideaTitle?: string;
  employeeName?: string;
  categoryName?: string;
  assignmentStage?: number;
  currentIdeaStatus?: string;
  reviewerDecision?: string;
  assignedDate?: string;
}

export interface ReviewerDecisionRequest {
  reviewerId?: number;
  feedback: string;
  decision: "ACCEPTED" | "REJECTED" | "REFINE";
}

export interface ReviewerDiscussionDTO {
  userActivityId: number;
  userId: number;
  displayName?: string;
  commentText: string;
  stageId: number;
  replyParent?: number | null;
  createdAt: string;
}

export interface ReviewerDiscussionRequestDTO {
  userId: number;
  stageId: number;
  text: string;
  replyParent?: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ProgressionStep {
  key: string;
  label: string;
  reached: boolean;
  active: boolean;
  at?: string | null;
}

export interface ProgressionBar {
  fromKey: string;
  toKey: string;
  filled: boolean;
}

export interface ProgressionDTO {
  ideaId: number;
  ideaTitle?: string;
  ideaDescription?: string;
  thumbnailURL?: string;
  currentStatus: string;
  steps: ProgressionStep[];
  bars: ProgressionBar[];
}

export type IdeaStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "UNDERREVIEW"
  | "ACCEPTED"
  | "REJECTED"
  | "PROJECTPROPOSAL"
  | "APPROVED"
  | "REFINE"
  | "PENDING";

// ─── API Functions ────────────────────────────────────────────────────────────

/** GET /api/reviewer/me/dashboard?filter=ALL */
export const getReviewerDashboard = async (
  filter = "ALL"
): Promise<ReviewerDashboardDTO[]> => {
  const res = await restApi.get<ReviewerDashboardDTO[]>(
    `/api/reviewer/me/dashboard`,
    { params: { filter } }
  );
  return res.data;
};

/** GET /api/reviewer/ideas/{ideaId} — returns ProgressionDTO */
export const getIdeaProgression = async (
  ideaId: number
): Promise<ProgressionDTO> => {
  const res = await restApi.get<ProgressionDTO>(
    `/api/reviewer/ideas/${ideaId}`
  );
  return res.data;
};

/** POST /api/reviewer/ideas/{ideaId}/decision */
export const submitDecision = async (
  ideaId: number,
  body: ReviewerDecisionRequest
): Promise<void> => {
  await restApi.post(`/api/reviewer/ideas/${ideaId}/decision`, body);
};

/** GET /api/reviewer/idea/{ideaId}/decisions */
export const getDecisions = async (
  ideaId: number
): Promise<ReviewerDecisionRequest[]> => {
  const res = await restApi.get<ReviewerDecisionRequest[]>(
    `/api/reviewer/idea/${ideaId}/decisions`
  );
  return res.data;
};

/** POST /api/reviewer/ideas/{ideaId}/discussions */
export const postDiscussion = async (
  ideaId: number,
  body: ReviewerDiscussionRequestDTO
): Promise<void> => {
  await restApi.post(`/api/reviewer/ideas/${ideaId}/discussions`, body);
};

/** POST /api/reviewer/ideas/{ideaId}/discussions/v2 — NEW: Derives stageId from assignment */
export const postDiscussionV2 = async (
  ideaId: number,
  text: string,
  replyParent?: number | null
): Promise<void> => {
  const body: Record<string, unknown> = { text };
  if (replyParent != null) {
    body.replyParent = replyParent;
  }
  await restApi.post(`/api/reviewer/ideas/${ideaId}/discussions/v2`, body);
};

/** GET /api/reviewer/ideas/{ideaId}/discussions?stageId= */
export const getDiscussions = async (
  ideaId: number,
  stageId: number
): Promise<ReviewerDiscussionDTO[]> => {
  const res = await restApi.get<ReviewerDiscussionDTO[]>(
    `/api/reviewer/ideas/${ideaId}/discussions`,
    { params: { stageId } }
  );
  return res.data;
};

/** GET /api/reviewer/ideas/{ideaId}/discussions/page?stageId=&page=&size=&sort= */
export const getDiscussionsPaged = async (
  ideaId: number,
  stageId: number,
  page = 0,
  size = 10,
  sort = "createdAt,asc"
): Promise<PagedResponse<ReviewerDiscussionDTO>> => {
  console.log(`API: getDiscussionsPaged - ideaId=${ideaId}, stageId=${stageId}, page=${page}, size=${size}, sort=${sort}`);
  const res = await restApi.get<PagedResponse<ReviewerDiscussionDTO>>(
    `/api/reviewer/ideas/${ideaId}/discussions/page`,
    { params: { stageId, page, size, sort } }
  );
  console.log(`API Response:`, res.data);
  return res.data;
};

/** GET /api/reviewer/ideas/{ideaId}/discussions/page/v2?page=&size=&sort= (NEW - stageId derived from assignment) */
export const getDiscussionsPagedV2 = async (
  ideaId: number,
  page = 0,
  size = 10,
  sort = "createdAt,asc"
): Promise<PagedResponse<ReviewerDiscussionDTO>> => {
  console.log(`API: getDiscussionsPagedV2 - ideaId=${ideaId}, page=${page}, size=${size}, sort=${sort}`);
  const res = await restApi.get<PagedResponse<ReviewerDiscussionDTO>>(
    `/api/reviewer/ideas/${ideaId}/discussions/page/v2`,
    { params: { page, size, sort } }
  );
  console.log(`API Response (v2):`, res.data);
  return res.data;
};

/** POST /api/reviewer/jobs/assignments/eod — Admin: trigger EOD assignment */
export const runEodAssignmentNow = async (): Promise<void> => {
  await restApi.post(`/api/reviewer/jobs/assignments/eod`);
};
