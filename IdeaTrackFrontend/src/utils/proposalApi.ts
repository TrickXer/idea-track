// Proposal Management API
import restApi from "./restApi";

// ─── Types ───────────────────────────────────────────────────────

export type ProofMeta = {
  fileName: string;
  filePath: string;
  contentType: "application/pdf" | "image/jpeg";
  sizeBytes: number;
};



export type ObjectiveCreation = {
  objectiveSeq: number;
  title: string;
  description: string;
  mandatory: boolean;
  proof: ProofMeta;
};

export type ProposalCreateRequestDTO = {
  userId?: number;
  budget: number;
  timeLineStart: string; // yyyy-MM-dd
  timeLineEnd: string;   // yyyy-MM-dd
  objectives: ObjectiveCreation[] | string;
  objectivesProof?: string;
};

export type ProposalResponseDTO = {
  proposalId: number;
  ideaId: number;
  userId: number;
  budget: number;
  objectives?: any[];
  timeLineStart: string;
  timeLineEnd: string;
  ideaStatus: string;
  createdAt: string;
  updatedAt: string;
};

export type AcceptedIdeaDashboardDTO = {
  ideaId: number;
  ideaTitle: string;
  ideaDescription: string;
  ideaStatus: string;
  ideaCreatedAt: string;
  proposalId?: number;
  budget?: number;
  timeLineStart?: string;
  timeLineEnd?: string;
  proposalStatus?: string;
  proposalCreatedAt?: string;
};

export type ProposalUpdateRequestDTO = {
  budget?: number | null;
  timeLineStart?: string | null;
  timeLineEnd?: string | null;
  objectives?: ObjectiveCreation[] | null;
};

export type ProposalDecisionRequest = {
  decision: "APPROVED" | "REJECTED";
  comments?: string;
};


// ─── Employee Endpoints ──────────────────────────────────────────

export async function getAcceptedIdeas(userId: number): Promise<AcceptedIdeaDashboardDTO[]> {
  const res = await restApi.get(`/api/proposal/${userId}/accepted-ideas`);
  
  return res.data;
}

export async function convertIdeaToProposal(
  ideaId: number,
  body: ProposalCreateRequestDTO
): Promise<ProposalResponseDTO> {
  const res = await // RIGHT
restApi.post(`/api/proposal/ideas/${ideaId}/convert-to-proposal`, body);  return res.data;
}

export async function updateDraftProposal(
  proposalId: number,
  body: ProposalUpdateRequestDTO
): Promise<ProposalResponseDTO> {
  const res = await restApi.put(`/api/proposal/updateProposal/${proposalId}`, body);
  return res.data;
}

export async function deleteDraftProposal(proposalId: number): Promise<void> {
  await restApi.delete(`/api/proposal/deleteProposal/${proposalId}`);
}

export async function submitProposal(proposalId: number): Promise<any> {
  const res = await restApi.post(`/api/proposal/${proposalId}/submit`);
  return res.data;
}

// ─── Admin Endpoints ─────────────────────────────────────────────

export async function getAllProposals(params?: Record<string, any>): Promise<any> {
  const res = await restApi.get("/api/admin/proposals", { params });
  return res.data;
}

export async function getOverdueReviewers(params?: Record<string, any>): Promise<any> {
  const res = await restApi.get("/api/admin/reviewers/overdue", { params });
  return res.data;
}

export async function getHealthSummary(): Promise<any> {
  const res = await restApi.get("/api/admin/health/summary");
  return res.data;
}

export async function startProposalReview(proposalId: number): Promise<any> {
  const res = await restApi.post(`/api/adminReview/proposal/${proposalId}/start`);
  return res.data;
}

export async function getProposalReviewDetail(proposalId: number): Promise<any> {
  const res = await restApi.get(`/api/adminReview/proposal/${proposalId}/review`);
  return res.data;
}

export async function processProposalDecision(
  proposalId: number,
  body: ProposalDecisionRequest
): Promise<string> {
  const res = await restApi.post(`/api/adminReview/proposal/${proposalId}/decision`, body);
  return res.data?.message ?? "OK";
}

// ─── Review Objectives (Admin review page) ───────────────────────

export type ObjectivesResponse = {
  id?: number | string;
  objectiveSeq?: number;
  title?: string;
  description?: string;
  hasProof?: boolean;
  proofType?: string;
  mandatory?: boolean;
  updatedAt?: string;
  objectivesProof?: string;
  proofFileName?: string;
  proofContentType?: string;
  proofFilePath?: string;
  proofSizeBytes?: number;
};

export type Paged<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type GetReviewObjectivesParams = {
  proposalId: number;
  page?: number;
  pageSize?: number;
  sort?: string;
  hasProof?: boolean;
  mandatory?: boolean;
  search?: string;
};

export async function getReviewObjectives(
  params: GetReviewObjectivesParams
): Promise<Paged<ObjectivesResponse>> {
  const { proposalId, page = 1, pageSize = 20, sort = 'objectiveSeq,asc', ...rest } = params;
  // Backend: GET /api/adminReview/proposal/{proposalId}/review  (1-based page)
  const res = await restApi.get(`/api/adminReview/proposal/${proposalId}/review`, {
    params: { page, pageSize, sort, ...rest },
  });
  return res.data;
}

export type AdminHealthSummaryDto = {
  pendingProposals?: number;
  overdueReviewers?: number;
  totalUsers?: number;
  activeUsers?: number;
  [key: string]: any;
};
