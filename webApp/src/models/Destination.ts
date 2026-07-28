export type DestinationSortField =
  | "name"
  | "country"
  | "city"
  | "category"
  | "createdAt"
  | "updatedAt";

export type SortDirection = "asc" | "desc";

export interface Destination {
  id: string;
  name: string;
  country: string;
  city: string | null;
  description: string;
  category: string;
  latitude: number | null;
  longitude: number | null;
  coverImageUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PagedDestinationResponse {
  items: Destination[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface DestinationQuery {
  search?: string;
  country?: string;
  city?: string;
  category?: string;
  page: number;
  size: number;
  sortBy: DestinationSortField;
  sortDirection: SortDirection;
}

export interface ApiMessage {
  status: string;
  message: string;
}
