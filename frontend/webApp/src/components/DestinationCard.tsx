import type { Destination } from "../models/Destination";
import { Link } from "react-router-dom";

interface DestinationCardProps {
  destination: Destination;
}

export function DestinationCard({ destination }: DestinationCardProps) {
  const city = destination.city?.trim();
  const location = city
    ? `${city}, ${destination.country}`
    : destination.country;
  const imageUrl = destination.coverImageUrl?.trim();

  return (
    <article className="destination-card">
      {imageUrl ? (
        <img
          className="destination-image"
          src={imageUrl}
          alt={destination.name}
        />
      ) : (
        <div
          className="destination-image destination-image-placeholder"
          role="img"
          aria-label={`No image available for ${destination.name}`}
        >
          No image available
        </div>
      )}

      <div className="destination-content">
        <div className="destination-meta">
          <span>{destination.category}</span>
        </div>

        <h2>{destination.name}</h2>
        <p className="location">{location}</p>
        <p>{destination.description}</p>

        <Link className="card-link" to={`/destinations/${destination.id}`}>View destination</Link>
      </div>
    </article>
  );
}
