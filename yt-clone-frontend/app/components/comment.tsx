import { Avatar, Button } from "@heroui/react";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
import LocalizedFormat from "dayjs/plugin/localizedFormat";
import { ThumbsDown, ThumbsUp } from "lucide-react";
import {
  toggleDislikeComment,
  toggleLikeComment,
  type Rate,
} from "~/scripts/api";
dayjs.extend(LocalizedFormat);
dayjs.extend(RelativeTime);

export default function Comment({
  className,
  user,
  createdAt,
  content,
  likes,
  dislikes,
  commentId,
  videoId,
  initialRate,
  children,
}: {
  className: string;
  user: string;
  createdAt: string;
  content: string;
  likes: number;
  dislikes: number;
  commentId: string;
  videoId: string;
  initialRate?: Rate | null;
  children?: React.ReactNode;
}) {
  return (
    <div className={className}>
      <Avatar>
        <Avatar.Fallback>{user.at(0)}</Avatar.Fallback>
      </Avatar>
      <div>
        <p className="text-sm">
          <span>{user} </span>
          <span className="text-gray-500">{dayjs(createdAt).fromNow()}</span>
        </p>
        <div>
          <p>{content}</p>
          <div className="flex gap-1">
            <div className="flex items-center">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => toggleLikeComment(videoId, commentId)}
              >
                <ThumbsUp fill={initialRate === "LIKE" ? "black" : "none"} />
              </Button>
              {likes > 0 && <span className="text-xs">{likes}</span>}
            </div>
            <div className="flex items-center">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => toggleDislikeComment(videoId, commentId)}
              >
                <ThumbsDown
                  fill={initialRate === "DISLIKE" ? "black" : "none"}
                />
              </Button>
              {dislikes > 0 && <span className="text-xs">{dislikes}</span>}
            </div>
          </div>

          {children}
        </div>
      </div>
    </div>
  );
}
