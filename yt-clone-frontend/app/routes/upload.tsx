import axios from "axios";
import { useState } from "react";
import { getAccessToken } from "~/scripts/auth";

export default function Upload() {
  const [isUploading, setUploading] = useState(false);
  const [percentOfUpload, setPercentOfUpload] = useState(0);
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const file = e.target.files[0];
      const formData = new FormData();
      formData.append("file", file);

      setUploading(true);
      axios.post("http://localhost:8080/videos", formData, {
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

  return (
    <div>
      <p>Upload .mp4 video</p>
      <div>
        <input type="file" accept=".mp4" onChange={handleFileChange} />
        {isUploading && <p>Uploading... {percentOfUpload}%</p>}
      </div>
    </div>
  );
}
