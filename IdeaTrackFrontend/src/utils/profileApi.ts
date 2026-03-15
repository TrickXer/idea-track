import restApi from "./restApi";
import type { UserProfile } from "./types";

/** GET /api/profile/me – Fetch the logged-in user's full profile. */
export async function fetchMyProfile(): Promise<UserProfile> {
  const response = await restApi.get<UserProfile>("/api/profile/me");
  return response.data;
}

/** PUT /api/profile/me – Update profile fields. */
export async function updateProfile(data: {
  name?: string;
  phoneNo?: string;
  bio?: string;
  profileUrl?: string;
}): Promise<UserProfile> {
  const response = await restApi.put<UserProfile>("/api/profile/me", data);
  return response.data;
}

/** POST /api/profile/me/profile-photo – Upload or replace avatar. */
export async function uploadProfilePhoto(file: File): Promise<any> {
  const formData = new FormData();
  formData.append("file", file);
  const response = await restApi.post("/api/profile/me/profile-photo", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data;
}

/** DELETE /api/profile/me/profile-photo – Remove avatar. */
export async function deleteProfilePhoto(): Promise<any> {
  const response = await restApi.delete("/api/profile/me/profile-photo");
  return response.data;
}

/** PUT /api/profile/me/password – Change password. */
export async function changePassword(currentPassword: string, newPassword: string): Promise<any> {
  const response = await restApi.put("/api/profile/me/password", { currentPassword, newPassword });
  return response.data;
}

/** DELETE /api/profile/me – Soft-delete authenticated user. */
export async function deleteMyProfile(): Promise<any> {
  const response = await restApi.delete("/api/profile/me");
  return response.data;
}

/** GET /api/profile/departments – Fetch all department names (public).
 *  Backend returns { deptNames: string[] } wrapper. */
export async function fetchDepartments(): Promise<string[]> {
  const response = await restApi.get<{ deptNames: string[] }>("/api/profile/departments");
  return response.data.deptNames;
}

// ─── Gamification ────────────────────────────────────────────────

export async function getXPProfile(): Promise<any> {
  const response = await restApi.get("/api/gamification/me/xp");
  return response.data;
}

export async function getXPHistory(): Promise<any[]> {
  const response = await restApi.get("/api/gamification/me/xp/history");
  return response.data;
}

export async function getInteractions(): Promise<any[]> {
  const response = await restApi.get("/api/gamification/me/interactions");
  return response.data;
}

export async function getInteractionsPage(page = 0, size = 20): Promise<any> {
  const response = await restApi.get("/api/gamification/me/interactions/page", {
    params: { page, size },
  });
  return response.data;
}

export async function getInteractionsByTypePage(type: string, page = 0, size = 20): Promise<any> {
  const response = await restApi.get("/api/gamification/me/interactions/by-type/page", {
    params: { type, page, size },
  });
  return response.data;
}

// ─── Hierarchy ───────────────────────────────────────────────────

export async function getIdeaHierarchy(ideaId: number): Promise<any> {
  const response = await restApi.get(`/api/hierarchy/idea/${ideaId}`);
  return response.data;
}
