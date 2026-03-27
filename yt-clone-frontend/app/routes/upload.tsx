import { useState } from "react";
import type { Route } from "./+types/upload";
import { useFetcher } from "react-router";
import { startVideoUpload, updateVideo, uploadVideo } from "~/scripts/api";

export async function clientAction({ request }: Route.ClientActionArgs) {
  const formData = await request.formData();
  const title = formData.get("title") || "";
  const videoId = formData.get("videoId");

  if (videoId) {
    updateVideo(videoId as string, title as string);
  }

  return { ok: true };
}

export default function Upload() {
  let fetcher = useFetcher();

  const [percentOfUpload, setPercentOfUpload] = useState(0);
  const [title, setTitle] = useState("");
  const [videoId, setVideoId] = useState<string | null>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const file = e.target.files[0];
      setTitle(file.name);

      const res = await startVideoUpload(file.name);
      setVideoId(res.videoId);

      const formData = new FormData();
      formData.append("file", file);

      uploadVideo(res.videoId, file, (loaded, total) => {
        if (total) {
          setPercentOfUpload(Math.round((loaded * 100) / total));
        }
      });
    }
  };

  return (
    <div className="flex gap-5">
      <div>
        <fetcher.Form
          method="POST"
          className={
            "flex flex-col items-start " + (videoId ? "" : "invisible")
          }
        >
          {videoId && (
            <input type="hidden" name="videoId" value={videoId}></input>
          )}
          <label htmlFor="title">Title:</label>
          <input
            id="title"
            type="text"
            name="title"
            defaultValue={title}
          ></input>
          <button type="submit">
            {fetcher.state !== "idle" ? "Saving..." : "Save changes"}
          </button>
        </fetcher.Form>
      </div>
      <div>
        <p>Upload .mp4 video</p>
        <div>
          <input type="file" accept=".mp4" onChange={handleFileChange} />
          {videoId && <p>Uploading... {percentOfUpload}%</p>}
        </div>
      </div>
    </div>
  );
}
