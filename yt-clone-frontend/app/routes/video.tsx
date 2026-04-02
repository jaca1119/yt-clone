import type { Route } from "./+types/video";
import LocalizedFormat from "dayjs/plugin/localizedFormat";
import { useEffect, useRef, useState } from "react";
import {
  addComment,
  type Comment,
  getCommentReplies,
  getVideoComments,
  getVideoMetadata,
  trackView,
  type Video,
} from "~/scripts/api";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
import { Avatar, Button } from "@heroui/react";
import AddComment from "~/components/add-comment";
import { useAuth } from "react-oidc-context";
import { ChevronDown, ChevronUp } from "lucide-react";
dayjs.extend(LocalizedFormat);
dayjs.extend(RelativeTime);

export async function clientLoader({ params }: Route.ClientLoaderArgs) {
  const video = await getVideoMetadata(params.id);
  const comments = await getVideoComments(params.id);

  trackView(params.id).catch(console.error);

  return {
    video: video,
    comments: comments.comments,
    hasNext: comments.hasNext,
  };
}

clientLoader.hydrate = true;

export function HydrateFallback() {
  return <div>Loading video...</div>;
}

export async function clientAction({ request }: Route.ClientActionArgs) {
  const formData = await request.formData();
  const videoId = formData.get("videoId") as string;
  const comment = formData.get("comment") as string;
  const replyId = formData.get("replyId") as string | null;

  await addComment(videoId, comment, replyId);

  return { ok: true };
}

export default function Video({ loaderData }: Route.ComponentProps) {
  const {
    video,
    comments: initialComments,
    hasNext: initialHasNext,
  } = loaderData;

  const [comments, setComments] = useState<Comment[]>(initialComments);
  const [hasNext, setHasNext] = useState(initialHasNext);
  const [replyId, setReplyId] = useState<string | null>(null);
  const [replies, setReplies] = useState<{
    parentId: string;
    replies: Comment[];
  } | null>(null);
  const auth = useAuth();

  const currentOffset = useRef(10);

  useEffect(() => {
    setComments(initialComments);
    setHasNext(initialHasNext);
    currentOffset.current = 10;
    setReplyId(null);
  }, [initialComments, initialHasNext]);

  async function showMore() {
    const nextCommentsPage = await getVideoComments(
      video.id,
      currentOffset.current,
    );
    setComments((prev) => [...prev, ...nextCommentsPage.comments]);
    setHasNext(nextCommentsPage.hasNext);
    currentOffset.current += 10;
  }

  async function toggleReplies(commentId: string) {
    if (commentId === replies?.parentId) {
      setReplies(null);
    } else {
      const repliesPage = await getCommentReplies(video.id, commentId);
      setReplies({
        parentId: commentId,
        replies: repliesPage.comments,
      });
    }
  }

  if (!video) {
    return <div>Video not found</div>;
  }

  return (
    <div className="flex flex-col m-auto items-center w-full">
      <video
        className="w-full h-180"
        controls
        src={`http://localhost:8080/videos/${video.id}`}
        poster={`http://localhost:8080/videos/${video.id}/thumbnail`}
      ></video>
      <div className="self-start pl-10 w-1/2">
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
          {auth.isAuthenticated && <AddComment videoId={video.id}></AddComment>}
          <p className="font-bold text-xl">Comments</p>
          <div>
            {comments.map((c) => (
              <div key={c.id} className="flex gap-2 my-3">
                <Avatar>
                  <Avatar.Fallback>{c.createdBy.at(0)}</Avatar.Fallback>
                </Avatar>
                <div>
                  <p className="text-sm">
                    <span>{c.createdBy} </span>
                    <span className="text-gray-500">
                      {dayjs(c.createdAt).fromNow()}
                    </span>
                  </p>
                  <div>
                    <p>{c.content}</p>
                    {c.replyCount !== 0 && (
                      <Button
                        variant="ghost"
                        onClick={() => toggleReplies(c.id)}
                      >
                        Replies {c.replyCount}
                        {replies?.parentId === c.id ? (
                          <ChevronUp />
                        ) : (
                          <ChevronDown />
                        )}
                      </Button>
                    )}
                    {replies?.parentId === c.id && (
                      <div>
                        {replies.replies.map((r) => (
                          <div className="flex gap-2 my-3">
                            <Avatar>
                              <Avatar.Fallback>
                                {r.createdBy.at(0)}
                              </Avatar.Fallback>
                            </Avatar>
                            <div>
                              <p className="text-sm">
                                <span>{r.createdBy} </span>
                                <span className="text-gray-500">
                                  {dayjs(r.createdAt).fromNow()}
                                </span>
                              </p>
                              <p>{r.content}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                    {auth.isAuthenticated && (
                      <>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setReplyId(c.id)}
                        >
                          Reply
                        </Button>
                        {replyId === c.id && (
                          <AddComment videoId={video.id} replyId={c.id}>
                            Add reply:
                          </AddComment>
                        )}
                      </>
                    )}
                  </div>
                </div>
              </div>
            ))}
            {hasNext && (
              <>
                <Button onClick={showMore} variant="secondary">
                  Show more
                </Button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
