import type { Route } from "./+types/video";
export default function Video({ params }: Route.ComponentProps) {
  return (
    <div>
      <p>Video id: {params.id}</p>
      <video
        className="w-5xl"
        controls
        src={"http://localhost:8080/videos/" + params.id}
        poster={`http://localhost:8080/videos/${params.id}/thumbnail`}
      ></video>
    </div>
  );
}
