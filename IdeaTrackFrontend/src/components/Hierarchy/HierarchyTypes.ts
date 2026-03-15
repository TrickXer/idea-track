export interface UserProfileData {
  userId: number;
  name: string;
  email: string;
  phoneNo: string;
  bio: string;
  profileUrl: string | null;
  role: string;
  departmentName: string;
  totalXp?: number;
  level?: string;
}

export interface HierarchyNodeDTO {
  id: number;
  reviewerId: number | null;
  reviewerName: string | null;
  role: string | null;
  department: string | null;
  phoneNo?: string | null;
  email?: string | null;
  profileUrl?: string | null;
  bio?: string | null;
  stage: number;
  feedback: string | null;
  decision: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'REFINE';
  decisionAt: string | null;
  createdAt: string;
  totalXp?: number;
  level?: string;
}

export interface AdminInfoDTO {
  adminUserId: number;
  adminName: string;
  adminRole: string;
  adminDept: string;
  decision: string | null;
  decisionAt: string | null;
  adminEmail?: string;
  adminPhoneNo?: string;
  adminBio?: string;
  adminProfileUrl?: string;
  totalXp?: number;
  level?: string;
}

export interface OwnerInfoDTO {
  ownerUserId: number;
  ownerName: string;
  ownerRole: string;
  ownerDept: string;
  ownerProfileUrl?: string | null;
}

export interface TimelineEntry {
  title: string;
  date: string;
}

export interface IdeaHierarchyDTO {
  ideaId: number;
  title: string;
  description: string;
  status: string;
  owner: OwnerInfoDTO;
  admin: AdminInfoDTO;
  nodesByStage: Record<string, HierarchyNodeDTO[]>;
  timeline: TimelineEntry[];
}
