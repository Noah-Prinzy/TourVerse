import { useEffect, useState } from "react";
import { DestinationCard } from "../../components/DestinationCard";
import { EmptyState, ErrorState, LoadingState } from "../../components/StatusStates";
import type { Favorite } from "../../models/Community";
import { getFavorites } from "../../services/communityApi";

export function FavoritesPage() {
  const [items, setItems] = useState<Favorite[] | null>(null);
  const [error, setError] = useState("");
  const load = () => { setError(""); getFavorites().then(setItems).catch((e) => setError(e.message)); };
  useEffect(load, []);
  if (!items && !error) return <LoadingState label="Loading favorites..." />;
  if (!items) return <ErrorState message={error} onRetry={load} />;
  return <section className="destinations-section"><h1>Favorites</h1>
    {items.length === 0 ? <EmptyState>No saved destinations yet.</EmptyState> :
      <div className="destination-grid">{items.map((item) => <DestinationCard key={item.id} destination={item.destination} />)}</div>}
  </section>;
}
