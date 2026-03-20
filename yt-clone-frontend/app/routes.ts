import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("routes/home.tsx"),
  route("video/:id", "routes/video.tsx"),
  route("callback", "routes/callback.tsx"),
  route("upload", "routes/upload.tsx"),
  route("manage", "routes/manage.tsx"),
  route("manage/videos/:videoId/edit", "routes/video-edit.tsx"),
] satisfies RouteConfig;
