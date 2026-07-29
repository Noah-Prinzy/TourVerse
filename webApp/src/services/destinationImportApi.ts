import type {
  DestinationCandidate, DestinationImportBatch, DestinationImportQuery,
} from "../models/DestinationImport";
import { apiBaseUrl, sessionManager } from "./session";

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body) headers.set("Content-Type", "application/json");
  const response = await sessionManager.authenticatedFetch(`${apiBaseUrl()}${path}`, { ...init, headers });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null;
    throw new Error(body?.message || `Request failed (HTTP ${response.status}).`);
  }
  return response.json() as Promise<T>;
}

export const destinationImportApi = {
  search: (query: DestinationImportQuery) =>
    request<DestinationImportBatch>("/api/admin/destination-imports/search", {
      method: "POST", body: JSON.stringify(query),
    }),
  batches: () => request<DestinationImportBatch[]>("/api/admin/destination-imports"),
  candidates: (batchId?: string) =>
    request<DestinationCandidate[]>(
      `/api/admin/destination-imports/candidates${batchId ? `?batchId=${encodeURIComponent(batchId)}` : ""}`,
    ),
  candidate: (id: string) =>
    request<DestinationCandidate>(`/api/admin/destination-imports/candidates/${id}`),
  update: (id: string, body: Partial<DestinationCandidate>) =>
    request<DestinationCandidate>(`/api/admin/destination-imports/candidates/${id}`, {
      method: "PUT", body: JSON.stringify(body),
    }),
  approve: (id: string) =>
    request<DestinationCandidate>(`/api/admin/destination-imports/candidates/${id}/approve`, { method: "POST" }),
  reject: (id: string, reason: string) =>
    request<DestinationCandidate>(`/api/admin/destination-imports/candidates/${id}/reject`, {
      method: "POST", body: JSON.stringify({ reason }),
    }),
  link: (id: string, destinationId: string) =>
    request<DestinationCandidate>(`/api/admin/destination-imports/candidates/${id}/link`, {
      method: "POST", body: JSON.stringify({ destinationId }),
    }),
};
