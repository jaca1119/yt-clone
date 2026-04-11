import { Avatar } from "@heroui/react";
import dayjs from "dayjs";
import RelativeTime from "dayjs/plugin/relativeTime";
import LocalizedFormat from "dayjs/plugin/localizedFormat";
dayjs.extend(LocalizedFormat);
dayjs.extend(RelativeTime);

export default function Comment({
  className,
  user,
  createdAt,
  content,
  children,
}: {
  className: string;
  user: string;
  createdAt: string;
  content: string;
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
          {children}
        </div>
      </div>
    </div>
  );
}
