import type { Route } from "./+types/home";
import { VideosList } from "../videos-list/videos-list";
import { getAllVideos } from "~/scripts/api";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "New React Router App" },
    { name: "description", content: "Welcome to React Router!" },
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
