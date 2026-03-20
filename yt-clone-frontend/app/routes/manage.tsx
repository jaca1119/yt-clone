import { deleteVideo, getAllVideosForUser } from "~/scripts/api";
import type { Route } from "./+types/manage";
import Thumbnail from "~/videos-list/thumbnail";
import { useFetcher } from "react-router";

export async function clientLoader() {
  return getAllVideosForUser();
}

export async function clientAction({ request }: Route.ClientActionArgs) {
  const formData = await request.formData();
  const videoId = formData.get("videoId") as string;

  await deleteVideo(videoId);

  return { ok: true };
}

export default function Manage({ loaderData }: Route.ComponentProps) {
  const fetcher = useFetcher();

  return (
    <div>
      <p>Manage</p>
      <div className="flex flex-col gap-3">
        {loaderData.map((v) => (
          <div key={v.id} className="flex gap-2">
            <Thumbnail
              className="w-28"
              videoId={v.id}
              length={v.length}
            ></Thumbnail>
            <p>{v.title}</p>

            <fetcher.Form method="DELETE">
              <input type="hidden" name="videoId" value={v.id}></input>
              <button type="submit">Delete</button>
            </fetcher.Form>
          </div>
        ))}
      </div>
    </div>
  );
}
