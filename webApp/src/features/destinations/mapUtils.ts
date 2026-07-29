import type { Destination } from "../../models/Destination";

export type DestinationMapState =
  | "ready"
  | "missing-key"
  | "missing-coordinates"
  | "invalid-coordinates";

declare global {
  interface Window {
    google?: {
      maps: {
        Map: new (element: HTMLElement, options: Record<string, unknown>) => unknown;
        Marker: new (options: Record<string, unknown>) => unknown;
      };
    };
  }
}

let mapsPromise: Promise<NonNullable<Window["google"]>> | null = null;

export function validCoordinates(
  latitude: number | null,
  longitude: number | null,
): boolean {
  return latitude != null && longitude != null &&
    Number.isFinite(latitude) && Number.isFinite(longitude) &&
    latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
}

export function destinationMapState(
  apiKey: string | undefined,
  latitude: number | null,
  longitude: number | null,
): DestinationMapState {
  if (latitude == null || longitude == null) return "missing-coordinates";
  if (!validCoordinates(latitude, longitude)) return "invalid-coordinates";
  return apiKey?.trim() ? "ready" : "missing-key";
}

export function googleMapsUrl(destination: Pick<
  Destination, "name" | "latitude" | "longitude" | "googlePlaceId"
>): string | null {
  if (!validCoordinates(destination.latitude, destination.longitude)) return null;
  const parameters = new URLSearchParams({
    api: "1",
    query: `${destination.latitude},${destination.longitude}`,
  });
  if (destination.googlePlaceId?.trim()) {
    parameters.set("query_place_id", destination.googlePlaceId.trim());
  }
  return `https://www.google.com/maps/search/?${parameters.toString()}`;
}

export function loadGoogleMaps(apiKey: string): Promise<NonNullable<Window["google"]>> {
  if (window.google?.maps) return Promise.resolve(window.google);
  if (mapsPromise) return mapsPromise;
  mapsPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>("script[data-tourverse-google-maps]");
    const script = existing ?? document.createElement("script");
    const onLoad = () => window.google?.maps
      ? resolve(window.google)
      : reject(new Error("Google Maps loaded without the expected browser API."));
    const onError = () => {
      mapsPromise = null;
      reject(new Error("Google Maps could not be loaded."));
    };
    script.addEventListener("load", onLoad, { once: true });
    script.addEventListener("error", onError, { once: true });
    if (!existing) {
      script.dataset.tourverseGoogleMaps = "true";
      script.async = true;
      script.defer = true;
      script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&loading=async`;
      document.head.appendChild(script);
    }
  });
  return mapsPromise;
}
