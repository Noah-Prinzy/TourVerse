import type { Destination } from "./Destination";

export interface Category {
  id: string; name: string; slug: string; description: string | null;
  iconUrl: string | null; active: boolean; createdAt: string; updatedAt: string;
}
export interface Review {
  id: string; userId: string; destinationId: string; rating: number;
  comment: string | null; createdAt: string; updatedAt: string;
}
export interface ReviewSummary { averageRating: number; reviewCount: number; reviews: Review[]; }
export interface Favorite { id: string; destination: Destination; createdAt: string; }
export interface TripDestination {
  id: string; destination: Destination; visitDate: string | null; notes: string | null; displayOrder: number;
}
export interface Trip {
  id: string; title: string; description: string | null; startDate: string | null;
  endDate: string | null; destinations: TripDestination[]; createdAt: string; updatedAt: string;
}
export interface TripInput { title: string; description?: string; startDate?: string | null; endDate?: string | null; }
