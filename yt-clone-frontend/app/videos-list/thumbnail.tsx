export default function Thumbnail({
  className,
  videoId,
  length,
}: {
  className?: string;
  videoId: string;
  length: number;
}) {
  return (
    <div className={"flex relative " + (className || "")}>
      <img
        className="m-auto"
        src={`http://localhost:8080/videos/${videoId}/thumbnail`}
      />
      <span className="p-1 bg-black text-white rounded-xl absolute right-1 bottom-1">
        {length >= 3600
          ? `${Math.floor(length / 3600)}:${Math.floor((length % 3600) / 60)
              .toString()
              .padStart(2, "0")}:${(length % 60).toString().padStart(2, "0")}`
          : `${Math.floor(length / 60)}:${(length % 60).toString().padStart(2, "0")}`}
      </span>
    </div>
  );
}
