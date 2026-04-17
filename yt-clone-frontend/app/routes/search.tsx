import type { Route } from "./+types/search";
import { searchVideos } from "~/scripts/api";
import { VideosList } from "~/videos-list/videos-list";

export function meta({}: Route.MetaArgs) {
  return [{ title: "Search - Yt-clone" }];
}

export async function clientLoader({ request }: Route.ClientLoaderArgs) {
  const url = new URL(request.url);
  const query = url.searchParams.get("q")?.trim() ?? "";

  if (!query) {
    return { query, videos: [] };
  }

  const videos = await searchVideos(query);
  return { query, videos };
}

export default function Search({ loaderData }: Route.ComponentProps) {
  return (
    <div>
      <p className="font-bold text-xl">Search</p>
      <p className="mt-2">
        {loaderData.query
          ? `Showing results for "${loaderData.query}"`
          : "Type something in the search bar to search."}
      </p>
      {loaderData.query && loaderData.videos.length === 0 ? (
        <p className="mt-2">No videos found.</p>
      ) : (
        <div className="mt-4">
          <VideosList videos={loaderData.videos} />
        </div>
      )}
    </div>
  );
}
