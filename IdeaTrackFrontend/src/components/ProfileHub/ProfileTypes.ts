export type Role = 'EMPLOYEE' | 'REVIEWER' | 'ADMIN' | 'SUPERADMIN';

export interface UserActivityDTO {
  userActivityId: number;
  delta: number;
  commentText?: string;
  activityType: 'COMMENT' | 'VOTE' | 'IDEA_STATUS' | 'SUBMISSION';
  event: string;
  createdAt: string;
}

export interface UserProfileDTO {
  userId: number;
  name: string;
  email: string;
  phoneNo: string;
  bio: string;
  profileUrl: string | null;
  role: Role;
  departmentName: string;
  totalXP: number;
  level: string;
  xpToNext: number;
  badges: string[];
  isProfileCompleted: boolean;
  completionPercent: number;
}
