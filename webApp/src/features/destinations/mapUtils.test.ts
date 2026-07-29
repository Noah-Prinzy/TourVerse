import { describe, expect, it } from "vitest";
import { destinationMapState, googleMapsUrl, validCoordinates } from "./mapUtils";

describe("destination map boundary", () => {
  it("handles missing key, coordinates, and invalid coordinates", () => {
    expect(destinationMapState(undefined, 0.3, 32.5)).toBe("missing-key");
    expect(destinationMapState("key", null, null)).toBe("missing-coordinates");
    expect(destinationMapState("key", 91, 32.5)).toBe("invalid-coordinates");
    expect(destinationMapState("key", 0.3, 32.5)).toBe("ready");
  });

  it("validates coordinate bounds", () => {
    expect(validCoordinates(-90, 180)).toBe(true);
    expect(validCoordinates(-91, 0)).toBe(false);
    expect(validCoordinates(0, Number.NaN)).toBe(false);
  });

  it("builds an encoded Google Maps URL with an optional place ID", () => {
    const url = googleMapsUrl({
      name: "Queen Elizabeth National Park",
      latitude: -0.2,
      longitude: 29.9,
      googlePlaceId: "ChIJ_example",
    });
    expect(url).toContain("query=-0.2%2C29.9");
    expect(url).toContain("query_place_id=ChIJ_example");
    expect(googleMapsUrl({ name: "Invalid", latitude: 200, longitude: 0 })).toBeNull();
  });
});
