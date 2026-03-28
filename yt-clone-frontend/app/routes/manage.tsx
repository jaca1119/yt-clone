import { deleteVideo, getAllVideosForUser } from "~/scripts/api";
import type { Route } from "./+types/manage";
import Thumbnail from "~/videos-list/thumbnail";
import { useFetcher } from "react-router";
import { Link } from "react-router";
import { Button, Table } from "@heroui/react";
import dayjs from "dayjs";

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
      <p className="font-bold text-xl">Manage</p>
      <Table>
        <Table.ScrollContainer>
          <Table.Content className="min-w-150">
            <Table.Header>
              <Table.Column>Video</Table.Column>
              <Table.Column>Title</Table.Column>
              <Table.Column>Upload date</Table.Column>
              <Table.Column>Actions</Table.Column>
            </Table.Header>
            <Table.Body>
              {loaderData.map((v) => (
                <Table.Row key={v.id}>
                  <Table.Cell>
                    <Thumbnail
                      className="w-28"
                      videoId={v.id}
                      length={v.length}
                    ></Thumbnail>
                  </Table.Cell>
                  <Table.Cell>{v.title}</Table.Cell>
                  <Table.Cell>
                    {dayjs(v.uploadDate).format("DD MMM YYYY")}
                  </Table.Cell>
                  <Table.Cell>
                    <div className="flex gap-1">
                      <fetcher.Form method="DELETE">
                        <input
                          type="hidden"
                          name="videoId"
                          value={v.id}
                        ></input>
                        <Button variant="secondary" type="submit">
                          Delete
                        </Button>
                      </fetcher.Form>

                      <Link
                        className="button button--secondary"
                        to={`videos/${v.id}/edit`}
                      >
                        Edit
                      </Link>
                    </div>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Content>
        </Table.ScrollContainer>
      </Table>
    </div>
  );
}
