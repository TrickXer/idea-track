
export interface IdeaCreateRequest {
  title: string;
  description: string;
  problemStatement: string;
  categoryId: number;
  tag: string;
  thumbnailURL: string;
}