import type { ApiMessage } from "../models/Auth";
import type { Destination } from "../models/Destination";
import type { Category, Favorite, Review, ReviewSummary, Trip, TripInput } from "../models/Community";
import { jsonRequest } from "./authApi";
import { apiBaseUrl } from "./session";

export const isUuid = (value: string | undefined): value is string =>
  Boolean(value && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value));

async function publicGet<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl()}${path}`);
  if (!response.ok) {
    const body = await response.json().catch(() => null) as Partial<ApiMessage> | null;
    throw new Error(body?.message || `Request failed (HTTP ${response.status}).`);
  }
  return response.json() as Promise<T>;
}

export const getDestination = (id: string) => publicGet<Destination>(`/api/destinations/${id}`);
export const getCategories = () => publicGet<Category[]>("/api/categories");
export const getReviews = (id: string) => publicGet<ReviewSummary>(`/api/destinations/${id}/reviews`);
export const createReview = (id: string, rating: number, comment: string) =>
  jsonRequest<Review>(`/api/destinations/${id}/reviews`, { method: "POST", body: JSON.stringify({ rating, comment: comment || null }) }, true);
export const updateReview = (id: string, rating: number, comment: string) =>
  jsonRequest<Review>(`/api/reviews/${id}`, { method: "PUT", body: JSON.stringify({ rating, comment: comment || null }) }, true);
export const deleteReview = (id: string) => jsonRequest<ApiMessage>(`/api/reviews/${id}`, { method: "DELETE" }, true);
export const getFavorites = () => jsonRequest<Favorite[]>("/api/favorites", { method: "GET" }, true);
export const addFavorite = (id: string) => jsonRequest<Favorite>(`/api/favorites/${id}`, { method: "POST" }, true);
export const removeFavorite = (id: string) => jsonRequest<ApiMessage>(`/api/favorites/${id}`, { method: "DELETE" }, true);
export const getTrips = () => jsonRequest<Trip[]>("/api/trips", { method: "GET" }, true);
export const getTrip = (id: string) => jsonRequest<Trip>(`/api/trips/${id}`, { method: "GET" }, true);
export const createTrip = (input: TripInput) => jsonRequest<Trip>("/api/trips", { method: "POST", body: JSON.stringify(input) }, true);
export const updateTrip = (id: string, input: TripInput) => jsonRequest<Trip>(`/api/trips/${id}`, { method: "PUT", body: JSON.stringify(input) }, true);
export const deleteTrip = (id: string) => jsonRequest<ApiMessage>(`/api/trips/${id}`, { method: "DELETE" }, true);
export const addTripDestination = (tripId: string, destinationId: string, visitDate?: string, notes?: string, displayOrder = 0) =>
  jsonRequest<Trip>(`/api/trips/${tripId}/destinations`, { method: "POST", body: JSON.stringify({ destinationId, visitDate: visitDate || null, notes: notes || null, displayOrder }) }, true);
export const removeTripDestination = (tripId: string, destinationId: string) =>
  jsonRequest<Trip>(`/api/trips/${tripId}/destinations/${destinationId}`, { method: "DELETE" }, true);
