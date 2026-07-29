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
  countryCode?: string | null;
  city: string | null;
  description: string;
  category: string;
  latitude: number | null;
  longitude: number | null;
  coverImageUrl: string | null;
  createdAt: string;
  updatedAt: string;
  dataOrigin?: "TOURVERSE_CURATED" | "EXTERNAL" | "HYBRID" | "DEVELOPMENT_SEED";
  lastVerifiedAt?: string | null;
  verificationStatus?: "VERIFIED" | "PARTIALLY_VERIFIED" | "REVIEW_REQUIRED" | "REJECTED";
  attributionSummary?: string | null;
  mapAvailable?: boolean;
  googlePlaceId?: string | null;
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
  countryCode?: string;
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

export interface DestinationCountry {
  code: string;
  name: string;
  destinationCount: number;
}

export interface DestinationCountriesResponse {
  countries: DestinationCountry[];
}
