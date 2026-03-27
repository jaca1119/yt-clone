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
import { Avatar, Button, Label, TextArea } from "@heroui/react";
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
  const [shouldShowControls, setShowControls] = useState(false);

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

  function showControls() {
    setShowControls(true);
  }

  return (
    <div className="flex flex-col m-auto items-center w-full">
      {!!video && (
        <>
          <video
            className="w-full h-180"
            controls
            src={`http://localhost:8080/videos/${params.id}`}
            poster={`http://localhost:8080/videos/${params.id}/thumbnail`}
          ></video>
          <div className="self-start pl-10">
            <p className="font-bold text-2xl">{video.title}</p>

            <div className="flex gap-3 pt-1">
              <Avatar>
                <Avatar.Fallback>{video.creator.at(0)}</Avatar.Fallback>
              </Avatar>
              <div>
                <p className="font-bold">{video.creator}</p>
                <p>
                  <span>{video.viewsCount} views </span> -
                  <span> {dayjs(video.uploadDate).fromNow()}</span>
                </p>
              </div>
            </div>
            <div className="self-start">
              <fetcher.Form method="POST" className="flex flex-col gap-2">
                {video && (
                  <input type="hidden" name="videoId" value={video.id}></input>
                )}
                <Label htmlFor="comment">Add comment:</Label>
                <TextArea
                  id="comment"
                  onFocus={showControls}
                  name="comment"
                ></TextArea>
                {shouldShowControls && (
                  <Button className="self-end" type="submit">
                    Add
                  </Button>
                )}
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
        </>
      )}
    </div>
  );
}
