import "@videojs/react/video/skin.css";
import type { Route } from "./+types/video";
import LocalizedFormat from "dayjs/plugin/localizedFormat";
import { useEffect, useRef, useState } from "react";
import {
  addComment,
  type Comment,
  getCommentReplies,
  getSubscriptionCount,
  getUserVideoInteractions,
  getVideoComments,
  getVideoMetadata,
  getSubscriptionStatus,
  toggleDislike,
  toggleLike,
  toggleSubscription,
  trackView,
  type Video,
  type Rate,
  getUserCommentInteractions,
  type UserCommentInteraction,
} from "~/scripts/api";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
import { Avatar, Button, ButtonGroup } from "@heroui/react";
import AddComment from "~/components/add-comment";
import CommentComponent from "~/components/comment";
import { useAuth } from "react-oidc-context";
import { ChevronDown, ChevronUp, ThumbsDown, ThumbsUp } from "lucide-react";
import { createPlayer, videoFeatures } from "@videojs/react";
import { Video as VideoPlayer, VideoSkin } from "@videojs/react/video";
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

const Player = createPlayer({ features: videoFeatures });

export default function Video({ loaderData, params }: Route.ComponentProps) {
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
  const [rate, setRate] = useState<Rate | null>(null);
  const [subscribed, setSubscribed] = useState(false);
  const [subscriptionCount, setSubscriptionCount] = useState<number>(0);
  const [isSubscriptionLoading, setIsSubscriptionLoading] = useState(false);
  const [commentsInteractions, setCommentsInteractions] = useState<
    UserCommentInteraction[]
  >([]);
  const auth = useAuth();
  const currentUsername = auth.user?.profile.sub;
  const isOwnVideo = auth.isAuthenticated && currentUsername === video.creator;

  const currentOffset = useRef(10);

  useEffect(() => {
    setComments(initialComments);
    setHasNext(initialHasNext);
    currentOffset.current = 10;
    setReplyId(null);
  }, [initialComments, initialHasNext]);

  useEffect(() => {
    getSubscriptionCount(video.creator).then((count) => {
      setSubscriptionCount(count);
    });

    if (auth.isAuthenticated) {
      getUserVideoInteractions(params.id).then((res) => {
        setRate(res.rate);
      });
      if (!isOwnVideo) {
        getSubscriptionStatus(video.creator).then((res) => {
          setSubscribed(res.subscribed);
        });
      } else {
        setSubscribed(false);
      }
    } else {
      setSubscribed(false);
    }
  }, [params.id, video.creator, auth.isAuthenticated, isOwnVideo]);

  useEffect(() => {
    if (auth.isAuthenticated) {
      getUserCommentInteractions(
        params.id,
        comments
          .map((c) => c.id)
          .concat(replies?.replies.map((c) => c.id) || []),
      ).then((res) => {
        setCommentsInteractions(res);
      });
    }
  }, [params.id, currentOffset.current, replies]);

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

  function like(videoId: string) {
    toggleLike(videoId);
    setRate(rate === null ? "LIKE" : null);
  }

  function dislike(videoId: string) {
    toggleDislike(videoId);
    setRate(rate === null ? "DISLIKE" : null);
  }

  async function onToggleSubscription() {
    if (!auth.isAuthenticated || isSubscriptionLoading) {
      return;
    }

    setIsSubscriptionLoading(true);
    try {
      const res = await toggleSubscription(video.creator);
      setSubscribed(res.subscribed);
      const count = await getSubscriptionCount(video.creator);
      setSubscriptionCount(count);
    } finally {
      setIsSubscriptionLoading(false);
    }
  }

  if (!video) {
    return <div>Video not found</div>;
  }

  return (
    <div className="flex flex-col m-auto items-center w-full">
      <div className="w-full h-180">
        <Player.Provider>
          <VideoSkin>
            <VideoPlayer
              src={`http://localhost:8080/videos/${video.id}`}
              poster={`http://localhost:8080/videos/${video.id}/thumbnail`}
              playsInline
            >
              <track
                kind="metadata"
                label="thumbnails"
                src={`/videos/${video.id}/preview_thumbnails_vtt`}
                default
              />
            </VideoPlayer>
          </VideoSkin>
        </Player.Provider>
      </div>
      <div className="self-start pl-10 w-1/2">
        <p className="font-bold text-2xl">{video.title}</p>

        <div className="flex gap-3 pt-1">
          <Avatar>
            <Avatar.Fallback>{video.creator.at(0)}</Avatar.Fallback>
          </Avatar>
          <div>
            <p className="font-bold">{video.creator}</p>
            <p className="text-sm text-gray-500">{subscriptionCount} subscribers</p>
            <p>
              <span>{video.viewsCount} views </span> -
              <span> {dayjs(video.uploadDate).fromNow()}</span>
            </p>
          </div>
          {!isOwnVideo && (
            <div className="self-center">
              <Button
                variant="secondary"
                className={
                  subscribed
                    ? "!bg-black !text-white border border-black"
                    : "!bg-white !text-black border border-gray-300"
                }
                isDisabled={!auth.isAuthenticated || isSubscriptionLoading}
                onClick={onToggleSubscription}
              >
                {subscribed ? "Subscribed" : "Subscribe"}
              </Button>
            </div>
          )}
          <div className="ml-auto self-center">
            <ButtonGroup variant="tertiary" isDisabled={!auth.isAuthenticated}>
              <Button onClick={() => like(video.id)}>
                <ThumbsUp fill={rate === "LIKE" ? "black" : "none"} />
                <span className="text-xs font-semibold">
                  {video.likes !== 0 ? video.likes : "Like"}
                </span>
              </Button>
              <Button onClick={() => dislike(video.id)}>
                <ButtonGroup.Separator />
                <ThumbsDown fill={rate === "DISLIKE" ? "black" : "none"} />
                <span className="text-xs font-semibold">
                  {video.dislikes !== 0 ? video.dislikes : "Dislike"}
                </span>
              </Button>
            </ButtonGroup>
          </div>
        </div>
        <div className="self-start">
          {auth.isAuthenticated && <AddComment videoId={video.id}></AddComment>}
          <p className="font-bold text-xl">Comments</p>
          <div>
            {comments.map((c) => (
              <CommentComponent
                key={c.id}
                className="flex gap-2 my-3"
                content={c.content}
                user={c.createdBy}
                createdAt={c.createdAt}
                likes={c.likes}
                dislikes={c.dislikes}
                commentId={c.id}
                videoId={video.id}
                initialRate={
                  commentsInteractions.find((i) => i.commentId === c.id)?.rate
                }
              >
                {c.replyCount !== 0 && (
                  <Button variant="ghost" onClick={() => toggleReplies(c.id)}>
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
                      <CommentComponent
                        key={r.id}
                        className="flex gap-2 my-3"
                        content={r.content}
                        user={r.createdBy}
                        createdAt={r.createdAt}
                        commentId={r.id}
                        videoId={video.id}
                        likes={r.likes}
                        dislikes={r.dislikes}
                        initialRate={
                          commentsInteractions.find((i) => i.commentId === r.id)
                            ?.rate
                        }
                      />
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
              </CommentComponent>
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
