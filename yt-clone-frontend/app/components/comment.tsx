import { Avatar, Button } from "@heroui/react";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
import LocalizedFormat from "dayjs/plugin/localizedFormat";
import { ThumbsDown, ThumbsUp } from "lucide-react";
import { toggleDislikeComment, toggleLikeComment } from "~/scripts/api";
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
          <div className="flex">
            <div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => toggleLikeComment(videoId, commentId)}
              >
                <ThumbsUp />
              </Button>
              {likes > 0 && <span className="text-xs">{likes}</span>}
            </div>
            <div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => toggleDislikeComment(videoId, commentId)}
              >
                <ThumbsDown />
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
