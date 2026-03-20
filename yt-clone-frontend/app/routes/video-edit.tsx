import Thumbnail from "~/videos-list/thumbnail";
import type { Route } from "./+types/video-edit";
import { getVideoMetadata, updateVideo } from "~/scripts/api";
import { useFetcher } from "react-router";

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
  return (
    <div className="flex gap-5">
      <Thumbnail
        videoId={params.videoId}
        length={loaderData.length}
      ></Thumbnail>
      <fetcher.Form method="POST" className="flex flex-col items-start gap-2">
        <input type="hidden" name="videoId" value={params.videoId}></input>

        <label htmlFor="title">Title:</label>
        <input
          id="title"
          type="text"
          name="title"
          defaultValue={loaderData.title}
        ></input>
        <button type="submit">
          {fetcher.state !== "idle" ? "Saving..." : "Save changes"}
        </button>
      </fetcher.Form>
    </div>
  );
}
