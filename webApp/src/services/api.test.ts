import { afterEach, describe, expect, it, vi } from "vitest";
import type { DestinationQuery } from "../models/Destination";
import {
  buildDestinationSearchParams,
  getDestinations,
} from "./api";

const query: DestinationQuery = {
  search: "mara & wildlife",
  country: "Kenya",
  city: "",
  category: "Wildlife",
  page: 2,
  size: 10,
  sortBy: "name",
  sortDirection: "asc",
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("destination API", () => {
  it("serializes supported query parameters and omits blanks", () => {
    const parameters = buildDestinationSearchParams(query);

    expect(parameters.get("search")).toBe("mara & wildlife");
    expect(parameters.get("country")).toBe("Kenya");
    expect(parameters.has("city")).toBe(false);
    expect(parameters.get("category")).toBe("Wildlife");
    expect(parameters.get("page")).toBe("2");
    expect(parameters.get("size")).toBe("10");
    expect(parameters.get("sortBy")).toBe("name");
    expect(parameters.get("sortDirection")).toBe("asc");
  });

  it("rejects impossible pagination before sending", () => {
    expect(() =>
      buildDestinationSearchParams({ ...query, page: 0 }),
    ).toThrow("Page must be at least 1.");
    expect(() =>
      buildDestinationSearchParams({ ...query, size: 101 }),
    ).toThrow("Size must be between 1 and 100.");
  });

  it("parses a paginated destination response", async () => {
    const response = {
      items: [],
      page: 2,
      size: 10,
      totalItems: 0,
      totalPages: 0,
    };
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(response), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getDestinations(query)).resolves.toEqual(response);
    expect(fetchMock.mock.calls[0][0]).toContain(
      "search=mara+%26+wildlife",
    );
  });

  it("preserves a backend ApiMessage", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: "error",
            message: "Size must be between 1 and 100.",
          }),
          { status: 400 },
        ),
      ),
    );

    await expect(getDestinations(query)).rejects.toThrow(
      "Size must be between 1 and 100.",
    );
  });

  it("uses a stable fallback for malformed errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response("not-json", { status: 500 }),
      ),
    );

    await expect(getDestinations(query)).rejects.toThrow(
      "Request failed (HTTP 500).",
    );
  });

  it("maps network failures to a stable message", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new TypeError("Failed to fetch")),
    );

    await expect(getDestinations(query)).rejects.toThrow(
      "Unable to connect to TourVerse. Check your connection and try again.",
    );
  });
});
