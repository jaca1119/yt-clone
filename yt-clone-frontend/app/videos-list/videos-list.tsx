import { Link } from "react-router";
import type { Video } from "~/scripts/api";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
import Thumbnail from "./thumbnail";
import { Avatar, Skeleton } from "@heroui/react";
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
                <div className="flex gap-3 mt-1.5">
                  <Avatar>
                    <Avatar.Fallback>{video.creator.at(0)}</Avatar.Fallback>
                  </Avatar>
                  <div>
                    <p className="font-semibold">{video.title}</p>
                    <div className="text-sm text-gray-600">
                      <p>{video.creator}</p>
                      <p>
                        <span>{video.viewsCount} views </span> -
                        <span> {dayjs(video.uploadDate).fromNow()}</span>
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </Link>
          ))}
    </div>
  );
}
