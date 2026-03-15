import restApi from "./restApi";
import type {
  CategoryCreateRequest,
  CategoryUpdateRequest,
  CategoryResponse,
} from "./types";

const BASE = "/api/categories";

/** POST /api/categories – Create a new category. */
export async function createCategory(
  data: CategoryCreateRequest
): Promise<CategoryResponse> {
  const response = await restApi.post<CategoryResponse>(BASE, data);
  return response.data;
}

/** GET /api/categories – Fetch all active (non-deleted) categories. */
export async function fetchAllCategories(): Promise<CategoryResponse[]> {
  const response = await restApi.get<CategoryResponse[]>(BASE);
  return response.data;
}

/** GET /api/categories/:id – Fetch a single category by ID. */
export async function fetchCategoryById(
  id: number
): Promise<CategoryResponse> {
  const response = await restApi.get<CategoryResponse>(`${BASE}/${id}`);
  return response.data;
}

/** PATCH /api/categories/:id – Partial update of a category. */
export async function updateCategory(
  id: number,
  data: CategoryUpdateRequest
): Promise<CategoryResponse> {
  const response = await restApi.patch<CategoryResponse>(
    `${BASE}/${id}`,
    data
  );
  return response.data;
}

/** DELETE /api/categories/:id – Soft-delete a category. */
export async function deleteCategory(id: number): Promise<void> {
  await restApi.delete(`${BASE}/${id}`);
}
