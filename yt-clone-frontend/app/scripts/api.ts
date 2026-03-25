import axios from "axios";
import { getAccessToken } from "./auth";

export interface Video {
  id: string;
  title: string;
  length: number;
  uploadDate: string;
  creator: string;
  viewsCount: number;
}

export interface UploadVideoResponse {
  videoId: string;
}

export interface Comment {
  id: number;
  content: string;
  createdAt: string;
  createdBy: string;
}

interface CommentResponse {
  commentId: number;
}

interface CommentsPageOffset {
  comments: Comment[];
  hasNext: boolean;
}

export async function getAllVideos() {
  const res = await fetch("http://localhost:8080/videos");
  return (await res.json()) as Video[];
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

export async function addComment(videoId: string, comment: string) {
  const res = await axios.post<CommentResponse>(
    `http://localhost:8080/videos/${videoId}/comments`,
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
