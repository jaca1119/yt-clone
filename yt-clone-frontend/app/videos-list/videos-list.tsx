import { Link } from "react-router";
import type { Video } from "~/routes/home";
import { redirectToOauth2Authorization } from "~/scripts/auth";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
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
        : videos.map(({ id, title, length, uploadDate, creator }) => (
            <Link
              to={`/video/${id}`}
              key={id}
              state={{
                id: id,
                title: title,
                length: length,
                uploadDate: uploadDate,
                creator: creator,
              }}
            >
              <div className="w-105">
                <div className="relative">
                  <img src={`http://localhost:8080/videos/${id}/thumbnail`} />
                  <span className="p-1 bg-black text-white rounded-xl absolute right-1 bottom-1">
                    {length >= 3600
                      ? `${Math.floor(length / 3600)}:${Math.floor(
                          (length % 3600) / 60,
                        )
                          .toString()
                          .padStart(
                            2,
                            "0",
                          )}:${(length % 60).toString().padStart(2, "0")}`
                      : `${Math.floor(length / 60)}:${(length % 60).toString().padStart(2, "0")}`}
                  </span>
                </div>
                <div>{title}</div>
                <div>{dayjs(uploadDate).fromNow()}</div>
                <div>Uploaded by: {creator} </div>
              </div>
            </Link>
          ))}
    </div>
  );
}
