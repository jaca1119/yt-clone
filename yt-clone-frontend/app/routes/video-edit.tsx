import Thumbnail from "~/videos-list/thumbnail";
import type { Route } from "./+types/video-edit";
import { getVideoMetadata, updateVideo } from "~/scripts/api";
import { useFetcher } from "react-router";
import { Button, Input, Label, TextField } from "@heroui/react";
import { useState } from "react";

export async function clientLoader({ params }: Route.ClientLoaderArgs) {
  return getVideoMetadata(params.videoId);
}

export async function clientAction({ request }: Route.ClientActionArgs) {
  const formData = await request.formData();
  const title = (formData.get("title") || "") as string;
  const videoId = formData.get("videoId") as string;

  await updateVideo(videoId, title);

  return { ok: true };
}

export default function VideoEdit({
  params,
  loaderData,
}: Route.ComponentProps) {
  const fetcher = useFetcher();
  const [title, setTitle] = useState(loaderData.title);
  return (
    <div className="flex gap-5">
      <Thumbnail
        videoId={params.videoId}
        length={loaderData.length}
      ></Thumbnail>
      <fetcher.Form method="POST" className="flex flex-col items-start gap-2">
        <input type="hidden" name="videoId" value={params.videoId}></input>
        <TextField name="title" type="text">
          <Label>Title:</Label>
          <Input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          ></Input>
        </TextField>
        <Button type="submit">Save changes</Button>
      </fetcher.Form>
    </div>
  );
}
