// Idea Submission & Collaboration API
import restApi from "./restApi";
import type { IdeaCreateRequest } from "../components/idea/IdeaCreateRequest";

export const getIdeaByLoggedUser = (userId: number) =>
  restApi.get(`/api/ideas/user/${userId}`);

export const getCategories = () =>
  restApi.get("/api/categories");

export const saveDraft = (payload: IdeaCreateRequest) =>
  restApi.post("/api/ideas/insertIdea", payload);

export const updateIdea = (ideaId: number, payload: IdeaCreateRequest) =>
  restApi.put(`/api/ideas/editDraft/${ideaId}`, payload);

export const submitFinalIdea = (ideaId: number) =>
  restApi.post(`/api/ideas/submit/${ideaId}`);

export const deleteIdea = (ideaId: number) =>
  restApi.delete(`/api/ideas/deleteIdea/${ideaId}`);

export const getAllIdeas = (query: string = "") =>
  restApi.get(`/api/ideas${query}`);

export const getIdeaDetails = (ideaId: number, viewerUserId?: number) =>
  restApi.get(`/api/ideas/${ideaId}`, { params: { viewerUserId } });

export const getComments = (ideaId: number) =>
  restApi.get(`/api/ideas/getComments/${ideaId}`);

export const postComment = (ideaId: number, payload: { text: string }) =>
  restApi.post(`/api/ideas/postComments/${ideaId}`, payload);

export const postVote = (ideaId: number, payload: { voteType: string }) =>
  restApi.post(`/api/ideas/vote/${ideaId}`, payload);

export const saveIdea = (ideaId: number) =>
  restApi.post(`/api/ideas/save/${ideaId}`);
