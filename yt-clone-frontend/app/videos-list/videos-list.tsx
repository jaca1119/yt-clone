import { Link } from "react-router";
import type { Video } from "~/scripts/api";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
import Thumbnail from "./thumbnail";
dayjs.extend(RelativeTime);

export function VideosList({
  videos,
  isLoading = false,
}: {
  videos?: Video[];
  isLoading?: boolean;
}) {
  return (
    <div className="grid grid-cols-[repeat(auto-fill,minmax(420px,1fr))] gap-6  w-8/10">
      {isLoading || !videos
        ? [...Array(5).keys()].map((i) => (
            <div key={i}>
              <div className="w-105 aspect-video animate-pulse bg-linear-to-r from-gray-200 via-gray-100 to-gray-200 dark:from-gray-800"></div>
              <div className="w-28 h-4 m-1 animate-pulse bg-linear-to-r from-gray-200 via-gray-100 to-gray-200 dark:from-gray-800"></div>
              <div className="w-20 h-4 m-1 animate-pulse bg-linear-to-r from-gray-200 via-gray-100 to-gray-200 dark:from-gray-800"></div>
              <div className="w-16 h-4 m-1 animate-pulse bg-linear-to-r from-gray-200 via-gray-100 to-gray-200 dark:from-gray-800"></div>
            </div>
          ))
        : videos.map((video) => (
            <Link to={`/video/${video.id}`} key={video.id} state={video}>
              <div className="w-105">
                <Thumbnail
                  className="aspect-video bg-black rounded-lg overflow-hidden"
                  videoId={video.id}
                  length={video.length}
                ></Thumbnail>
                <div>{video.title}</div>
                <div>{dayjs(video.uploadDate).fromNow()}</div>
                <div>Uploaded by: {video.creator} </div>
              </div>
            </Link>
          ))}
    </div>
  );
}
