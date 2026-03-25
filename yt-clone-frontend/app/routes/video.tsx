import { useLocation } from "react-router";
import type { Route } from "./+types/video";
import LocalizedFormat from "dayjs/plugin/localizedFormat";
import { useEffect, useState } from "react";
import {
  addComment,
  type Comment,
  getVideoComments,
  getVideoMetadata,
  trackView,
  type Video,
} from "~/scripts/api";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
import { useFetcher } from "react-router";
dayjs.extend(LocalizedFormat);
dayjs.extend(RelativeTime);

export async function clientAction({ request }: Route.ClientActionArgs) {
  const formData = await request.formData();
  const videoId = formData.get("videoId") as string;
  const comment = formData.get("comment") as string;

  await addComment(videoId, comment);

  return { ok: true };
}

export default function Video({ params }: Route.ComponentProps) {
  const location = useLocation();
  const fetcher = useFetcher();

  const [video, setVideo] = useState<Video | null>(location.state);
  const [comments, setComments] = useState<Comment[] | null>();
  const [hasNext, setHasNext] = useState(false);

  let currentOffset = 0;

  useEffect(() => {
    async function fetchVideoMetadata() {
      const data = await getVideoMetadata(params.id);
      setVideo(data);
    }

    async function fetchVideoComments() {
      const comments = await getVideoComments(params.id);
      setComments(comments.comments);
      setHasNext(comments.hasNext);
    }

    trackView(params.id);

    if (!video) {
      fetchVideoMetadata();
    }

    fetchVideoComments();
  }, [params.id]);

  function showMore() {
    currentOffset += 10;
    getVideoComments(params.id, currentOffset).then((c) => {
      if (comments) {
        setComments(comments.concat(c.comments));
        setHasNext(c.hasNext);
      }
    });
  }

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
            <p>Views: {video.viewsCount}</p>
          </div>
        </>
      )}
      <div className="self-start">
        <fetcher.Form method="POST">
          {video && (
            <input type="hidden" name="videoId" value={video.id}></input>
          )}
          <label htmlFor="comment">Add comment:</label>
          <textarea name="comment"></textarea>
          <button type="submit">Add</button>
        </fetcher.Form>
        <p className="font-bold text-xl">Comments</p>
        <div>
          {comments &&
            comments.map((c) => (
              <div key={c.id}>
                <p>
                  {c.content} by {c.createdBy} at:{" "}
                  {dayjs(c.createdAt).fromNow()}
                </p>
              </div>
            ))}
          {hasNext && (
            <>
              <button onClick={showMore}>Show more</button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
