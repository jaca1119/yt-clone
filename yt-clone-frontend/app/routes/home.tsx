import type { Route } from "./+types/home";
import { VideosList } from "../videos-list/videos-list";
import { getAllVideos } from "~/scripts/api";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Yt-clone" },
    { name: "description", content: "Welcome to yt-clone!" },
  ];
}

export async function clientLoader() {
  return await getAllVideos();
}

export function HydrateFallback() {
  return <VideosList isLoading />;
}

export default function Home({ loaderData }: Route.ComponentProps) {
  return <VideosList videos={loaderData} />;
}
