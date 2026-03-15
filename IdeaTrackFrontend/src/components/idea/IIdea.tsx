
export interface IIdea {
  ideaId: number;
  title: string;
  tag?: string;
  description: string;
  problemStatement: string;
  thumbnailURL:string;
  category: { name: string };
  author: { displayName: string };
  ideaStatus: string;
  stage: number;
  feedback:string[];
  votes: { upvotes: number; downvotes: number };
  commentsCount: number;
  createdAt: string; // ISO date string
}

export interface profile{
  name:string;
  userId:number;
}
