export type ImportStatus =
  | "PENDING_REVIEW" | "APPROVED" | "REJECTED"
  | "POSSIBLE_DUPLICATE" | "IMPORT_FAILED";

export interface DestinationImportBatch {
  id: string;
  provider: string;
  countryCode: string | null;
  city: string | null;
  queryText: string | null;
  requestedLimit: number;
  status: "RUNNING" | "COMPLETED" | "FAILED";
  retrievedCount: number;
  errorMessage: string | null;
  createdAt: string;
}

export interface DestinationCandidate {
  id: string;
  batchId: string;
  sourceProvider: string;
  externalId: string;
  sourceUrl: string | null;
  name: string;
  countryCode: string | null;
  country: string;
  city: string | null;
  categoryHint: string | null;
  mappedCategory: string | null;
  descriptionHint: string | null;
  imageReference: string | null;
  imageLicence: string | null;
  imageAttribution: string | null;
  reviewStatus: ImportStatus;
  rejectionReason: string | null;
  duplicateOfDestinationId: string | null;
  approvedDestinationId: string | null;
}

export interface DestinationImportQuery {
  provider: "WIKIDATA" | "OPENTRIPMAP";
  countryCode: string;
  city?: string;
  search?: string;
  limit: number;
}
