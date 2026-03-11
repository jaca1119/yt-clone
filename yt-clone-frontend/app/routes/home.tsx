import type { Route } from "./+types/home";
import { VideosList } from "../videos-list/videos-list";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "New React Router App" },
    { name: "description", content: "Welcome to React Router!" },
  ];
}

export interface Video {
  id: string;
  title: string;
  length: number;
}

export async function clientLoader() {
  try {
    const res = await fetch("http://localhost:8080/videos");
    return (await res.json()) as Video[];
  } catch {
    return [
      {
        id: "1",
        title: "title",
        length: 12,
      },
    ];
  }
}

export function HydrateFallback() {
  return <div>Loading...</div>;
}

export default function Home({ loaderData }: Route.ComponentProps) {
  return <VideosList videos={loaderData} />;
}
