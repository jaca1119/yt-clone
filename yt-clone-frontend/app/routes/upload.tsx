import axios from "axios";
import { useEffect, useState } from "react";
import { getAccessToken } from "~/scripts/auth";

interface UploadVideoResponse {
  videoId: string;
}

export default function Upload() {
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setUploading] = useState(false);
  const [percentOfUpload, setPercentOfUpload] = useState(0);
  const [title, setTitle] = useState("");
  const [videoId, setVideoId] = useState<string | null>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const file = e.target.files[0];
      setFile(file);
      setTitle(file.name);

      const res = await axios.post<UploadVideoResponse>(
        "http://localhost:8080/videos",
        {
          title: file.name,
        },
        {
          headers: {
            Authorization: "Bearer " + getAccessToken(),
          },
        },
      );
      setVideoId(res.data.videoId);

      const formData = new FormData();
      formData.append("file", file);

      setUploading(true);
      axios.post(`http://localhost:8080/videos/${res.data.videoId}`, formData, {
        headers: {
          Authorization: `Bearer ${getAccessToken()}`,
          "Content-Type": "multipart/form-data",
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            setPercentOfUpload(
              Math.round((progressEvent.loaded * 100) / progressEvent.total),
            );
          }
        },
      });
    }
  };

  function save(formData: FormData) {
    const title = formData.get("title") || "";
    setTitle(title as string);
    axios.put(
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

  return (
    <div className="flex gap-5">
      <div>
        <form
          className={
            "flex flex-col items-start " + (isUploading ? "" : "invisible")
          }
          action={save}
        >
          <label htmlFor="title">Title:</label>
          <input
            id="title"
            type="text"
            name="title"
            defaultValue={title}
          ></input>
          <button type="submit">Save</button>
        </form>
      </div>

      <div>
        <p>Upload .mp4 video</p>
        <div>
          <input type="file" accept=".mp4" onChange={handleFileChange} />
          {isUploading && <p>Uploading... {percentOfUpload}%</p>}
        </div>
      </div>
    </div>
  );
}
