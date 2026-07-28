import type {
  ApiMessage,
  DestinationQuery,
  PagedDestinationResponse,
} from "../models/Destination";

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

export function buildDestinationSearchParams(
  query: DestinationQuery,
): URLSearchParams {
  if (!Number.isInteger(query.page) || query.page < 1) {
    throw new RangeError("Page must be at least 1.");
  }
  if (!Number.isInteger(query.size) || query.size < 1 || query.size > 100) {
    throw new RangeError("Size must be between 1 and 100.");
  }

  const parameters = new URLSearchParams();
  const optionalParameters = {
    search: query.search,
    country: query.country,
    city: query.city,
    category: query.category,
  };

  for (const [name, value] of Object.entries(optionalParameters)) {
    const cleanValue = value?.trim();
    if (cleanValue) {
      parameters.set(name, cleanValue);
    }
  }

  parameters.set("page", String(query.page));
  parameters.set("size", String(query.size));
  parameters.set("sortBy", query.sortBy);
  parameters.set("sortDirection", query.sortDirection);
  return parameters;
}

async function readApiError(response: Response): Promise<string> {
  const fallback = `Request failed (HTTP ${response.status}).`;

  try {
    const text = await response.text();
    if (!text.trim()) {
      return fallback;
    }
    const parsed = JSON.parse(text) as Partial<ApiMessage>;
    return typeof parsed.message === "string" && parsed.message.trim()
      ? parsed.message
      : fallback;
  } catch {
    return fallback;
  }
}

export async function getDestinations(
  query: DestinationQuery,
  signal?: AbortSignal,
): Promise<PagedDestinationResponse> {
  const parameters = buildDestinationSearchParams(query);
  let response: Response;

  try {
    response = await fetch(
      `${API_BASE_URL}/api/destinations?${parameters.toString()}`,
      { signal },
    );
  } catch (reason) {
    if (reason instanceof DOMException && reason.name === "AbortError") {
      throw reason;
    }
    throw new Error(
      "Unable to connect to TourVerse. Check your connection and try again.",
    );
  }

  if (!response.ok) {
    throw new Error(await readApiError(response));
  }

  try {
    return (await response.json()) as PagedDestinationResponse;
  } catch {
    throw new Error("TourVerse returned an invalid destination response.");
  }
}
