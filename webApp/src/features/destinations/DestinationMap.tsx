import { useEffect, useRef, useState } from "react";
import type { Destination } from "../../models/Destination";
import {
  destinationMapState,
  googleMapsUrl,
  loadGoogleMaps,
} from "./mapUtils";

export function DestinationMap({ destination }: { destination: Destination }) {
  const element = useRef<HTMLDivElement>(null);
  const [failure, setFailure] = useState("");
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
  const state = destinationMapState(apiKey, destination.latitude, destination.longitude);
  const externalUrl = googleMapsUrl(destination);

  useEffect(() => {
    if (state !== "ready" || !element.current) return;
    let active = true;
    setFailure("");
    loadGoogleMaps(apiKey!).then((google) => {
      if (!active || !element.current) return;
      const position = { lat: destination.latitude!, lng: destination.longitude! };
      const map = new google.maps.Map(element.current, {
        center: position,
        zoom: 13,
        mapTypeControl: false,
        streetViewControl: false,
      });
      new google.maps.Marker({ map, position, title: destination.name });
    }).catch((reason) => {
      if (active) setFailure(reason instanceof Error ? reason.message : "Google Maps is unavailable.");
    });
    return () => { active = false; };
  }, [apiKey, destination.id, destination.latitude, destination.longitude, destination.name, state]);

  const message = failure ||
    (state === "missing-key" ? "Interactive map unavailable: no web Maps key is configured." :
      state === "missing-coordinates" ? "Map unavailable: this destination has no coordinates." :
        state === "invalid-coordinates" ? "Map unavailable: destination coordinates are invalid." : "");

  return <section className="destination-map-section" aria-label={`Map for ${destination.name}`}>
    <h2>Map</h2>
    {state === "ready" && !failure
      ? <div ref={element} className="destination-map" role="region" aria-label={`Google Map showing ${destination.name}`} />
      : <div className="map-fallback" role="status">{message}</div>}
    {externalUrl && <a href={externalUrl} target="_blank" rel="noreferrer">Open in Google Maps</a>}
  </section>;
}
