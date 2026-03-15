// Reviewer Stage Assignment API
import restApi from "./restApi";

// ─── Types ───────────────────────────────────────────────────────

export interface IReviewerAssignment {
  reviewerId: number;
  categoryId: number;
  stageNo: number;
}

export interface IAssignedReviewer {
  reviewerId: number;
  name: string;
  categoryId: number;
  categoryName: string;
  stageNo: number;
}

export interface IAvailableReviewer {
  userId: number;
  name: string;
  deptName: string;
}

export interface ICategory {
  categoryId: number;
  categoryName: string;
  stageCount: number;
}

export interface IViewDept {
  deptId: number;
  deptName: string;
}

// ─── Endpoints ───────────────────────────────────────────────────

export const getAvailableReviewersList = (deptId: number) =>
  restApi.get(`/api/reviewerAssignment/getAvailableReviewersList/${deptId}`);

export const getCategoriesAndStageCountByCategory = (deptId: number) =>
  restApi.get(`/api/reviewerAssignment/getCategoriesAndStageCountByCategory/${deptId}`);

export const assignReviewerToStage = (reviewerObj: IReviewerAssignment) =>
  restApi.post("/api/reviewerAssignment/assignReviewerToStage", reviewerObj);

export const assignedReviewerDetails = () =>
  restApi.get("/api/reviewerAssignment/assignedReviewerDetails");

export const removeReviewerFromStage = (
  reviewerId: number,
  categoryId: number,
  stageNo: number
) =>
  restApi.delete(
    `/api/reviewerAssignment/removeReviewerFromStage?reviewerId=${reviewerId}&categoryId=${categoryId}&stageNo=${stageNo}`
  );
