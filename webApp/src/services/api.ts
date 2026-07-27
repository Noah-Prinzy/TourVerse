import type { Destination } from "../models/Destination";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function getDestinations(): Promise<Destination[]> {
  const response = await fetch(`${API_BASE_URL}/api/destinations`);

  if (!response.ok) {
    throw new Error("Failed to load destinations.");
  }

  return response.json() as Promise<Destination[]>;
}
