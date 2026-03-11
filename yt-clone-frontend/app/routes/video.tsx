import { useLocation } from "react-router";
import type { Route } from "./+types/video";
import dayjs from "dayjs";
import LocalizedFormat from "dayjs/plugin/localizedFormat";
import { useEffect, useState } from "react";
import type { Video } from "./home";
dayjs.extend(LocalizedFormat);

export default function Video({ params }: Route.ComponentProps) {
  const location = useLocation();

  const [video, setVideo] = useState(location.state as Video);

  useEffect(() => {
    async function fetchVideoMetadata() {
      const res = await fetch(
        `http://localhost:8080/videos/${params.id}/metadata`,
      );
      const data = await res.json();
      setVideo(data);
    }

    if (!video) {
      fetchVideoMetadata();
    }
  });

  return (
    <div className="flex flex-col m-auto items-center w-5xl">
      <video
        controls
        src={`http://localhost:8080/videos/${params.id}`}
        poster={`http://localhost:8080/videos/${params.id}/thumbnail`}
      ></video>
      <div className="self-start">
        <p className="text-2xl font-bold">{video.title}</p>
        <p>Uploaded: {dayjs(video.uploadDate).format("LL")}</p>
      </div>
    </div>
  );
}
