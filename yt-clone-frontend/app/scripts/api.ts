import axios from "axios";
import { getAccessToken } from "./auth";

export interface Video {
  id: string;
  title: string;
  length: number;
  uploadDate: string;
  creator: string;
  viewsCount: number;
  likes: number;
  dislikes: number;
}

interface SearchVideosResponse {
  videos: Video[];
}

export interface UploadVideoResponse {
  videoId: string;
}

export interface Comment {
  id: string;
  content: string;
  createdAt: string;
  createdBy: string;
  replyCount: number;
  likes: number;
  dislikes: number;
}

interface CommentResponse {
  commentId: number;
}

interface CommentsPageOffset {
  comments: Comment[];
  hasNext: boolean;
}

export type Rate = "LIKE" | "DISLIKE";

interface UserVideoInteractions {
  rate: Rate | null;
}

export interface UserCommentInteraction {
  commentId: string;
  rate: Rate | null;
}

interface SubscriptionStatus {
  subscribed: boolean;
}

export async function getAllVideos() {
  const res = await fetch("http://localhost:8080/videos");
  return (await res.json()) as Video[];
}

export async function getVideosFeed() {
  const res = await fetch("http://localhost:8080/videos/feed");
  return (await res.json()) as Video[];
}

export async function searchVideos(query: string) {
  const params = new URLSearchParams({ q: query });
  const res = await fetch(`http://localhost:8080/videos/search?${params}`);
  const data = (await res.json()) as SearchVideosResponse;
  return data.videos;
}

export async function updateVideo(videoId: string, title: string) {
  await axios.put(
    `http://localhost:8080/videos/${videoId}`,
    {
      title: title,
    },
    {
      headers: {
        Authorization: `Bearer ${getAccessToken()}`,
      },
    },
  );
}

export async function startVideoUpload(title: string) {
  const res = await axios.post<UploadVideoResponse>(
    "http://localhost:8080/videos",
    {
      title: title,
    },
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );
  return res.data;
}

export async function uploadVideo(
  videoId: string,
  file: File,
  onProgress: (loaded: number, total?: number) => void,
) {
  const formData = new FormData();
  formData.append("file", file);

  axios.post(`http://localhost:8080/videos/${videoId}`, formData, {
    headers: {
      Authorization: `Bearer ${getAccessToken()}`,
      "Content-Type": "multipart/form-data",
    },
    onUploadProgress: (progressEvent) => {
      onProgress(progressEvent.loaded, progressEvent.total);
    },
  });
}

export async function getVideoMetadata(videoId: string) {
  const res = await fetch(`http://localhost:8080/videos/${videoId}/metadata`);
  return (await res.json()) as Video;
}

export async function getAllVideosForUser() {
  const res = await axios.get<Video[]>("http://localhost:8080/videos/by-user", {
    headers: {
      Authorization: "Bearer " + getAccessToken(),
    },
  });

  return res.data;
}

export async function deleteVideo(videoId: string) {
  return axios.delete(`http://localhost:8080/videos/${videoId}`, {
    headers: {
      Authorization: "Bearer " + getAccessToken(),
    },
  });
}

export async function getVideoComments(videoId: string, offset?: number) {
  const res = await axios.get<CommentsPageOffset>(
    `http://localhost:8080/videos/${videoId}/comments/newest?offset=${offset || ""}`,
  );

  return res.data;
}

export async function getCommentReplies(
  videoId: string,
  parentId: string,
  offset?: number,
) {
  const res = await axios.get<CommentsPageOffset>(
    `http://localhost:8080/videos/${videoId}/comments/${parentId}/newest?offset=${offset || ""}`,
  );

  return res.data;
}

export async function addComment(
  videoId: string,
  comment: string,
  replyId: string | null,
) {
  const reply = replyId ? `/${replyId}` : "";
  const res = await axios.post<CommentResponse>(
    `http://localhost:8080/videos/${videoId}/comments${reply}`,
    {
      comment: comment,
    },
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );

  return res.data;
}

export async function trackView(videoId: string) {
  return await axios.post(`http://localhost:8080/videos/${videoId}/views`);
}

export async function toggleLike(videoId: string) {
  return await axios.post(
    `http://localhost:8080/videos/${videoId}/toggle-like`,
    null,
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );
}

export async function toggleDislike(videoId: string) {
  return await axios.post(
    `http://localhost:8080/videos/${videoId}/toggle-dislike`,
    null,
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );
}

export async function getUserVideoInteractions(videoId: string) {
  try {
    const res = await axios.get<UserVideoInteractions>(
      `http://localhost:8080/videos/${videoId}/user-interactions`,
      {
        headers: {
          Authorization: "Bearer " + getAccessToken(),
        },
      },
    );

    return res.data;
  } catch (err) {
    return {
      rate: null,
    };
  }
}

export async function toggleLikeComment(videoId: string, commentId: string) {
  return await axios.post(
    `http://localhost:8080/videos/${videoId}/comments/${commentId}/toggle-like`,
    null,
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );
}

export async function toggleDislikeComment(videoId: string, commentId: string) {
  return await axios.post(
    `http://localhost:8080/videos/${videoId}/comments/${commentId}/toggle-dislike`,
    null,
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );
}

export async function getUserCommentInteractions(
  videoId: string,
  commentIds: string[],
) {
  const res = await axios.post<UserCommentInteraction[]>(
    `http://localhost:8080/videos/${videoId}/comments/user-interactions`,
    commentIds,
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );

  return res.data;
}

export async function getSubscriptionStatus(creatorUsername: string) {
  const res = await axios.get<SubscriptionStatus>(
    `http://localhost:8080/subscriptions/${creatorUsername}/status`,
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );

  return res.data;
}

export async function toggleSubscription(creatorUsername: string) {
  const res = await axios.post<SubscriptionStatus>(
    `http://localhost:8080/subscriptions/${creatorUsername}/toggle`,
    null,
    {
      headers: {
        Authorization: "Bearer " + getAccessToken(),
      },
    },
  );

  return res.data;
}

export async function getSubscriptionCount(creatorUsername: string) {
  const res = await axios.get<number>(
    `http://localhost:8080/subscriptions/${creatorUsername}/count`,
  );

  return res.data;
}
