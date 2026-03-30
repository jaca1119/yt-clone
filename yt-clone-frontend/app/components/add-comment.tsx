import { Button, Label, TextArea } from "@heroui/react";
import { useState } from "react";
import { useFetcher } from "react-router";

export default function AddComment({
  videoId,
  replyId,
  children,
}: {
  videoId: string;
  replyId?: string;
  children?: React.ReactNode;
}) {
  const fetcher = useFetcher();
  const [showControls, setShowControls] = useState(false);
  return (
    <fetcher.Form method="POST" className="flex flex-col gap-2">
      <input type="hidden" name="videoId" value={videoId}></input>
      {replyId && <input type="hidden" name="replyId" value={replyId}></input>}
      <Label htmlFor="comment">{children || "Add comment:"}</Label>
      <TextArea
        id="comment"
        onFocus={() => setShowControls(true)}
        name="comment"
      ></TextArea>
      {showControls && (
        <Button className="self-end" type="submit">
          Add
        </Button>
      )}
    </fetcher.Form>
  );
}
