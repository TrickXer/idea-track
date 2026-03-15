// src/components/ProfileHub/ProfileMapper.ts
import type { UserProfileDTO as UIUserProfile } from './ProfileTypes';

type ApiUserProfile = {
  userId: number;
  name: string;
  email: string;
  phoneNo: string;
  bio: string;
  profileUrl: string | null;
  role: 'EMPLOYEE' | 'REVIEWER' | 'ADMIN' | 'SUPERADMIN' | string;
  departmentName: string;
  totalXP: number;
  level: string;
  xpToNextLevel?: number;
  badges: string[];
  profileCompleted?: boolean;
  profileCompletionPercent?: number;
  [k: string]: any;
};

export function mapApiToUIProfile(api: ApiUserProfile): UIUserProfile {
  return {
    userId: api.userId,
    name: api.name,
    email: api.email,
    phoneNo: api.phoneNo,
    bio: api.bio,
    profileUrl: api.profileUrl,
    role: (api.role as UIUserProfile['role']) ?? 'EMPLOYEE',
    departmentName: api.departmentName,
    totalXP: api.totalXP,
    level: api.level,
    xpToNext: typeof api.xpToNextLevel === 'number' ? api.xpToNextLevel : 0,
    badges: Array.isArray(api.badges) ? api.badges : [],
    isProfileCompleted: Boolean(api.profileCompleted),
    completionPercent:
      typeof api.profileCompletionPercent === 'number'
        ? Math.max(0, Math.min(100, api.profileCompletionPercent))
        : 0,
  };
}
