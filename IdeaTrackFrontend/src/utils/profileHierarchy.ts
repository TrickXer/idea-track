// Re-export all profile API functions from the central profileApi
// This keeps backward compatibility for components that import from profileHierarchy
export {
  fetchMyProfile as getMyProfile,
  updateProfile,
  uploadProfilePhoto,
  deleteProfilePhoto,
  changePassword,
  deleteMyProfile as deleteProfile,
  deleteMyProfile,
  fetchDepartments,
  getXPProfile,
  getXPHistory,
  getInteractions,
  getInteractionsPage,
  getInteractionsByTypePage,
  getIdeaHierarchy,
} from "./profileApi";

// Kept for components that import getPublicProfile
import restApi from "./restApi";
export const getPublicProfile = async (userId: number) => {
  const response = await restApi.get(`/api/profile/public/${userId}`);
  return response.data;
};
