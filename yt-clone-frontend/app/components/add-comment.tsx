import { Button, Label, TextArea } from "@heroui/react";
import { useEffect, useRef, useState } from "react";
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
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (fetcher.state === "idle" && fetcher.data?.ok) {
      if (formRef.current) {
        formRef.current.reset();
      }

      setShowControls(false);
      fetcher.reset();
    }
  }, [fetcher.state, fetcher.data, fetcher]);

  return (
    <fetcher.Form ref={formRef} method="POST" className="flex flex-col gap-2">
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
