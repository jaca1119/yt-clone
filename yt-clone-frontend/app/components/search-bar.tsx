import { SearchField } from "@heroui/react";
import { Form } from "react-router";

export default function SearchBar() {
  return (
    <Form method="GET" action="/search">
      <SearchField aria-label="Search videos">
        <SearchField.Group>
          <SearchField.SearchIcon />
          <SearchField.Input
            name="q"
            className="w-70"
            placeholder="Search..."
          />
          <SearchField.ClearButton />
        </SearchField.Group>
      </SearchField>
    </Form>
  );
}
