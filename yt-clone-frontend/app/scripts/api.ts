import axios from "axios";
import { getAccessToken } from "./auth";

export interface Video {
  id: string;
  title: string;
  length: number;
  uploadDate: string;
  creator: string;
}

export interface UploadVideoResponse {
  videoId: string;
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
