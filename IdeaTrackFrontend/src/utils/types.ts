/* ===================================================================
   Shared TypeScript types / interfaces for the IdeaTrack frontend.
   Maps 1-to-1 with the Spring Boot backend DTOs.
   =================================================================== */

// ─── User / Auth ─────────────────────────────────────────────────

export type CreateUserPayload = {
  name: string;
  email: string;
  deptName?: string;
  role: "ADMIN" | "REVIEWER" | "EMPLOYEE";
};

// ─── User / Profile ──────────────────────────────────────────────

export interface UserProfile {
  userId: number;
  name: string;
  email: string;
  phoneNo: string;
  bio: string;
  profileUrl: string;
  role: string;
  departmentName: string;
  totalXP: number;
  level: string;
  xpToNextLevel: number;
  badges: string[];
  profileCompleted: boolean;
  profileCompletionPercent: number;
}

export interface DepartmentMiniDTO {
  deptId: number;
  deptName: string;
}

export interface UserMiniDTO {
  userId: number;
  name: string;
  email: string;
}

// ─── Category ────────────────────────────────────────────────────

export interface CategoryCreateRequest {
  name: string;
  departmentId: number;
  createdByAdminId: number;
  reviewerCountPerStage: number;
  stageCount: number;
}

export interface CategoryUpdateRequest {
  name?: string;
  departmentId?: number;
  createdByAdminId?: number;
  reviewerCountPerStage?: number;
  stageCount?: number;
}

export interface CategoryResponse {
  categoryId: number;
  department: DepartmentMiniDTO;
  name: string;
  createdByAdmin: UserMiniDTO;
  reviewerCountPerStage: number;
  stageCount: number;
  createdAt: string;
  updatedAt: string;
  deleted: boolean;
}

// ─── Idea ────────────────────────────────────────────────────────

export interface CategoryLiteDTO {
  categoryId: number;
  name: string;
}

export interface UserLiteDTO {
  userId: number;
  displayName: string;
  avatarUrl?: string;
}

export interface VoteCountsDTO {
  upvotes: number;
  downvotes: number;
}

export type VoteType = "UPVOTE" | "DOWNVOTE";

export interface ViewerStatusDTO {
  saved?: boolean;
  voteType?: VoteType;
  owner?: boolean;
}

export interface IdeaResponse {
  ideaId: number;
  title: string;
  description: string;
  problemStatement?: string;
  tag?: string;
  thumbnailURL?: string;
  category: CategoryLiteDTO;
  author: UserLiteDTO;
  ideaStatus: IdeaStatus;
  stage?: number;
  feedback?: string[];
  votes?: VoteCountsDTO;
  commentsCount: number;
  viewer?: ViewerStatusDTO;
  createdAt: string;
  updatedAt: string;
  deleted: boolean;
}

// ─── Bulk Idea Operations ────────────────────────────────────────

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

export interface BulkIdeaActionRequest {
  ideaIds: number[];
  ideaStatus?: IdeaStatus;
  categoryId?: number;
  delete?: boolean;
  tag?: string;
  clearTag?: boolean;
  reviewerFeedback?: string;
  clearReviewerFeedback?: boolean;
  thumbnailURL?: string;
}

export interface BulkActionResult {
  requestedCount: number;
  foundCount: number;
  updatedCount: number;
  updatedIds: number[];
  notFoundIds: number[];
  warnings: string[];
}

export interface BulkExportRequest {
  ideaIds: number[];
}

// ─── Notifications ───────────────────────────────────────────────

/**
 * The parsed shape of Notification.metadata JSON.
 * Backend builds this in NotificationHelper.buildMetadata().
 *
 * Examples:
 *   { redirectTo: "/my-idea/8",  triggeredBy: { userId: 1, name: "Reviewer" },
 *     context: { ideaId: 8, ideaTitle: "...", stage: 2, action: "IDEA_APPROVED" } }
 *   { redirectTo: "/profile",    triggeredBy: { userId: 0, name: "System" },
 *     context: { badgeName: "Top Innovator", action: "BADGE_EARNED" } }
 */
export interface NotificationTriggeredBy {
  userId: number;
  name: string;
}

export interface NotificationContext {
  // Idea actions
  ideaId?: number;
  ideaTitle?: string;
  stage?: number;
  // Proposal actions
  proposalId?: number;
  // Badge/level actions
  badgeName?: string;
  level?: string;
  // Vote
  voteType?: "UPVOTE" | "DOWNVOTE";
  // Reviewer
  reviewerName?: string;
  // Challenge / deadline
  challengeId?: number;
  challengeTitle?: string;
  daysRemaining?: number;
  // Discriminator – always present, drives icon & display logic in the frontend
  action:
    | "IDEA_SUBMITTED"
    | "IDEA_APPROVED"
    | "IDEA_REJECTED"
    | "IDEA_REFINE"
    | "IDEA_STAGE_ADVANCED"
    | "COMMENT_ADDED"
    | "VOTE_CAST"
    | "FEEDBACK_POSTED"
    | "REVIEWER_ASSIGNED"
    | "REVIEWER_DISCUSSION"
    | "REVIEWER_SLA_EXPIRED"
    | "PROPOSAL_SUBMITTED"
    | "PROPOSAL_APPROVED"
    | "PROPOSAL_REJECTED"
    | "BADGE_EARNED"
    | "LEVEL_UP"
    | "DEADLINE_REMINDER"
    | "COLLABORATION_INVITE"
    | "MENTION"
    | string; // allow future actions without breaking existing code
}

export interface NotificationMetadata {
  redirectTo: string;
  triggeredBy: NotificationTriggeredBy;
  context: NotificationContext;
}

/** Parse the raw metadata JSON string. Returns null if invalid / absent. */
export function parseNotificationMetadata(raw: string | null | undefined): NotificationMetadata | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as NotificationMetadata;
  } catch {
    return null;
  }
}

export interface NotificationCreateRequest {
  userId: number;
  notificationType: string;
  notificationTitle: string;
  notificationMessage: string;
  priority: "LOW" | "MEDIUM" | "HIGH";
  metadata?: string;
}

export interface BulkNotificationRequest {
  items: NotificationCreateRequest[];
}

export interface NotificationResponse {
  notificationId: number;
  userId: number;
  notificationType: string;
  notificationTitle: string;
  notificationMessage: string;
  priority: string;
  notificationStatus: "UNREAD" | "READ";
  pushed: boolean;
  metadata: string;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationSSEEvent {
  notificationId: number;
  type: string;
  title: string;
  message: string;
  priority: string;
  metadata: string;
  createdAtIso: string;
}

export interface MarkReadRequest {
  userId: number;
  notificationIds: number[];
}

export interface MarkAllReadRequest {
  userId: number;
}

// ─── Pagination ──────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// ─── Notification query params ───────────────────────────────────

export interface NotificationQueryParams {
  userId: number;
  status?: "UNREAD" | "READ" | "ALL";
  pushed?: boolean | null;
  from?: string; // ISO-8601
  to?: string;   // ISO-8601
  page?: number;
  size?: number;
  sort?: string;
}
