import { Link } from "react-router";
import type { Video } from "~/routes/home";
import { redirectToOauth2Authorization } from "~/scripts/auth";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
dayjs.extend(RelativeTime);

export function VideosList({ videos }: { videos: Video[] }) {
  return (
    <div className="flex flex-wrap gap-3 ml-5">
      {videos.map(({ id, title, length, uploadDate }) => (
        <Link
          to={`/video/${id}`}
          key={id}
          state={{
            id: id,
            title: title,
            length: length,
            uploadDate: uploadDate,
          }}
        >
          <div className="relative">
            <img
              className="w-[320px] h-45"
              src={`http://localhost:8080/videos/${id}/thumbnail`}
            />
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
        </Link>
      ))}
    </div>
  );
}
