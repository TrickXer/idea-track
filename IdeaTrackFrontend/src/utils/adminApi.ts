// User management API calls
import restApi from "./restApi";
import type { CreateUserPayload } from "./types";

export async function getDepartments(): Promise<{ deptNames: string[] }> {
  const res = await restApi.get<{ deptNames: string[] }>("/api/profile/departments");
  return res.data;
}

export async function listUsers(): Promise<any[]> {
  const res = await restApi.get("/api/admin/all");
  return res.data;
}

export async function createUser(payload: CreateUserPayload): Promise<any> {
  const res = await restApi.post("/api/admin/create", payload);
  return res.data;
}

export async function updateUser(id: number, payload: Partial<CreateUserPayload & { phoneNo?: string; bio?: string; profileUrl?: string }>): Promise<any> {
  const res = await restApi.put(`/api/admin/${id}`, payload);
  return res.data;
}

export async function deleteUser(id: number): Promise<any> {
  const res = await restApi.delete(`/api/admin/${id}`);
  return res.data;
}

export async function updateUserStatus(userId: number, status: "ACTIVE" | "INACTIVE"): Promise<any> {
  const res = await restApi.patch(`/api/users/${userId}/status`, { status });
  return res.data;
}
