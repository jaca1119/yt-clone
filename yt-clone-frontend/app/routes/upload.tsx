import { useState } from "react";
import type { Route } from "./+types/upload";
import { useFetcher } from "react-router";
import { startVideoUpload, updateVideo, uploadVideo } from "~/scripts/api";
import {
  Button,
  FieldGroup,
  Fieldset,
  Input,
  Label,
  ProgressBar,
  TextField,
} from "@heroui/react";

export async function clientAction({ request }: Route.ClientActionArgs) {
  const formData = await request.formData();
  const title = formData.get("title") || "";
  const videoId = formData.get("videoId");

  if (videoId) {
    updateVideo(videoId as string, title as string);
  }

  return { ok: true };
}

export default function Upload() {
  let fetcher = useFetcher();

  const [percentOfUpload, setPercentOfUpload] = useState(0);
  const [title, setTitle] = useState("");
  const [videoId, setVideoId] = useState<string | null>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const file = e.target.files[0];
      setTitle(file.name);

      const res = await startVideoUpload(file.name);
      setVideoId(res.videoId);

      const formData = new FormData();
      formData.append("file", file);

      uploadVideo(res.videoId, file, (loaded, total) => {
        if (total) {
          setPercentOfUpload(Math.round((loaded * 100) / total));
        }
      });
    }
  };

  return (
    <div className="flex gap-5">
      <div className="flex flex-col">
        <Label htmlFor="file">Upload .mp4 video</Label>
        <Input
          id="file"
          type="file"
          accept=".mp4"
          onChange={handleFileChange}
        ></Input>
        {videoId && (
          <ProgressBar value={percentOfUpload}>
            <Label>Uploading</Label>
            <ProgressBar.Output></ProgressBar.Output>
            <ProgressBar.Track>
              <ProgressBar.Fill></ProgressBar.Fill>
            </ProgressBar.Track>
          </ProgressBar>
        )}
      </div>
      <div>
        <fetcher.Form
          method="POST"
          className={
            "flex flex-col items-start " + (videoId ? "" : "invisible")
          }
        >
          {videoId && (
            <input type="hidden" name="videoId" value={videoId}></input>
          )}
          <Fieldset>
            <Fieldset.Legend>Video details</Fieldset.Legend>
            <FieldGroup>
              <TextField name="title" type="text">
                <Label>Title:</Label>
                <Input
                  placeholder="asd"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                ></Input>
              </TextField>
            </FieldGroup>
            <Fieldset.Actions>
              <Button type="submit">
                {fetcher.state !== "idle" ? "Saving..." : "Save changes"}
              </Button>
            </Fieldset.Actions>
          </Fieldset>
        </fetcher.Form>
      </div>
    </div>
  );
}
