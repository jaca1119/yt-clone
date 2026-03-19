import { useLocation } from "react-router";
import type { Route } from "./+types/video";
import LocalizedFormat from "dayjs/plugin/localizedFormat";
import { useEffect, useState } from "react";
import { getVideoMetadata, type Video } from "~/scripts/api";
import dayjs from "dayjs";
dayjs.extend(LocalizedFormat);

export default function Video({ params }: Route.ComponentProps) {
  const location = useLocation();

  const [video, setVideo] = useState<Video | null>(location.state);

  useEffect(() => {
    async function fetchVideoMetadata() {
      const data = await getVideoMetadata(params.id);
      setVideo(data);
    }

    if (!video) {
      fetchVideoMetadata();
    }
  });

  return (
    <div className="flex flex-col m-auto items-center w-5xl">
      {!!video && (
        <>
          <video
            controls
            src={`http://localhost:8080/videos/${params.id}`}
            poster={`http://localhost:8080/videos/${params.id}/thumbnail`}
          ></video>
          <div className="self-start">
            <p className="text-2xl font-bold">{video.title}</p>
            <p>Uploaded: {dayjs(video.uploadDate).format("LL")}</p>
            <p>By: {video.creator}</p>
          </div>
        </>
      )}
    </div>
  );
}
