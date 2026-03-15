import restApi from "./restApi";
import type {
  BulkIdeaActionRequest,
  BulkActionResult,
  BulkExportRequest,
  IdeaResponse,
  PageResponse,
  IdeaStatus,
} from "./types";

const BASE = "/api/ideas/bulk";

/** GET /api/ideas – Fetch paginated list of all ideas with optional filters. */
export async function fetchAllIdeas(params?: {
  q?: string;
  categoryId?: number;
  userId?: number;
  status?: IdeaStatus;
  includeDeleted?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}): Promise<PageResponse<IdeaResponse>> {
  const response = await restApi.get<PageResponse<IdeaResponse>>("/api/ideas", {
    params: {
      page: params?.page ?? 0,
      size: params?.size ?? 200,
      sort: params?.sort ?? "createdAt,desc",
      includeDeleted: params?.includeDeleted ?? false,
      ...(params?.q && { q: params.q }),
      ...(params?.categoryId && { categoryId: params.categoryId }),
      ...(params?.userId && { userId: params.userId }),
      ...(params?.status && { status: params.status }),
    },
  });
  return response.data;
}

/** POST /api/ideas/bulk/actions – Apply bulk operations to selected ideas. */
export async function bulkIdeaActions(
  data: BulkIdeaActionRequest
): Promise<BulkActionResult> {
  const response = await restApi.post<BulkActionResult>(
    `${BASE}/actions`,
    data
  );
  return response.data;
}

/** POST /api/ideas/bulk/export – Export selected idea IDs to CSV (returns blob). */
export async function exportIdeasByIds(
  data: BulkExportRequest
): Promise<Blob> {
  const response = await restApi.post(`${BASE}/export`, data, {
    responseType: "blob",
  });
  return response.data;
}

/** GET /api/ideas/bulk/export/category/:categoryId – Export ideas in a category. */
export async function exportIdeasByCategory(
  categoryId: number,
  includeDeleted = false
): Promise<Blob> {
  const response = await restApi.get(
    `${BASE}/export/category/${categoryId}`,
    {
      params: { includeDeleted },
      responseType: "blob",
    }
  );
  return response.data;
}

/** GET /api/ideas/bulk/export/all – Export all ideas platform-wide. */
export async function exportAllIdeas(includeDeleted = false): Promise<Blob> {
  const response = await restApi.get(`${BASE}/export/all`, {
    params: { includeDeleted },
    responseType: "blob",
  });
  return response.data;
}

/** Helper: trigger browser download for a CSV blob. */
export function downloadCsvBlob(blob: Blob, filename?: string) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download =
    filename ?? `ideas_export_${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
