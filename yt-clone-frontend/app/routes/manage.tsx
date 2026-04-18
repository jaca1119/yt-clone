import { deleteVideo, getAllVideosForUser } from "~/scripts/api";
import type { Route } from "./+types/manage";
import Thumbnail from "~/videos-list/thumbnail";
import { useFetcher } from "react-router";
import { Link } from "react-router";
import { AlertDialog, Button, Table, type SortDescriptor } from "@heroui/react";
import dayjs from "dayjs";
import { useMemo, useState } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";

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
  const [sortDescriptor, setSortDescriptor] = useState<SortDescriptor>({
    column: "uploadDate",
    direction: "descending",
  });

  const sortedVideos = useMemo(() => {
    const { column, direction } = sortDescriptor;
    const isDescending = direction === "descending";

    return [...loaderData].sort((a, b) => {
      if (column === "title") {
        const result = a.title.localeCompare(b.title, undefined, {
          sensitivity: "base",
        });
        return isDescending ? -result : result;
      }

      const first = dayjs(a.uploadDate).valueOf();
      const second = dayjs(b.uploadDate).valueOf();
      return isDescending ? second - first : first - second;
    });
  }, [loaderData, sortDescriptor]);

  const renderSortIcon = (column: string) => {
    if (sortDescriptor.column !== column) {
      return <ArrowUpDown className="size-4 opacity-50" />;
    }

    return sortDescriptor.direction === "descending" ? (
      <ArrowDown className="size-4" />
    ) : (
      <ArrowUp className="size-4" />
    );
  };

  return (
    <div>
      <p className="font-bold text-xl">Manage</p>
      <Table>
        <Table.ScrollContainer>
          <Table.Content
            className="min-w-150"
            sortDescriptor={sortDescriptor}
            onSortChange={setSortDescriptor}
          >
            <Table.Header>
              <Table.Column>Video</Table.Column>
              <Table.Column id="title" isRowHeader allowsSorting>
                <span className="inline-flex items-center gap-1">
                  Title
                  {renderSortIcon("title")}
                </span>
              </Table.Column>
              <Table.Column id="uploadDate" allowsSorting>
                <span className="inline-flex items-center gap-1">
                  Upload date
                  {renderSortIcon("uploadDate")}
                </span>
              </Table.Column>
              <Table.Column>Actions</Table.Column>
            </Table.Header>
            <Table.Body>
              {sortedVideos.map((v) => (
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
                      <AlertDialog>
                        <Button variant="secondary">Delete</Button>
                        <AlertDialog.Backdrop>
                          <AlertDialog.Container>
                            <AlertDialog.Dialog className="sm:max-w-100">
                              <AlertDialog.CloseTrigger />
                              <AlertDialog.Header>
                                <AlertDialog.Icon status="danger" />
                                <AlertDialog.Heading>
                                  Delete video permanently?
                                </AlertDialog.Heading>
                              </AlertDialog.Header>
                              <AlertDialog.Body>
                                <p>
                                  This will permanently delete{" "}
                                  <strong>{v.title}</strong> and all of its
                                  data. This action cannot be undone.
                                </p>
                              </AlertDialog.Body>
                              <AlertDialog.Footer>
                                <Button slot="close" variant="tertiary">
                                  Cancel
                                </Button>
                                <fetcher.Form method="DELETE" slot="close">
                                  <input
                                    type="hidden"
                                    name="videoId"
                                    value={v.id}
                                  ></input>
                                  <Button variant="danger" type="submit">
                                    Delete
                                  </Button>
                                </fetcher.Form>
                              </AlertDialog.Footer>
                            </AlertDialog.Dialog>
                          </AlertDialog.Container>
                        </AlertDialog.Backdrop>
                      </AlertDialog>
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
