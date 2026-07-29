import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { ErrorState, LoadingState } from "../../components/StatusStates";
import type { Destination } from "../../models/Destination";
import type { ReviewSummary } from "../../models/Community";
import {
  addFavorite, createReview, deleteReview, getDestination, getFavorites, getReviews,
  isUuid, removeFavorite, updateReview,
} from "../../services/communityApi";
import { useAuth } from "../../state/AuthContext";
import { DestinationMap } from "./DestinationMap";

export function DestinationDetailsPage() {
  const { destinationId } = useParams();
  const { user } = useAuth();
  const [destination, setDestination] = useState<Destination | null>(null);
  const [summary, setSummary] = useState<ReviewSummary | null>(null);
  const [favorite, setFavorite] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");

  const load = async () => {
    if (!isUuid(destinationId)) { setError("The destination ID is invalid."); setLoading(false); return; }
    setLoading(true); setError("");
    try {
      const [item, reviews] = await Promise.all([getDestination(destinationId), getReviews(destinationId)]);
      setDestination(item); setSummary(reviews);
      if (user) setFavorite((await getFavorites()).some((entry) => entry.destination.id === destinationId));
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to load destination."); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, [destinationId, user?.id]);

  if (loading) return <LoadingState label="Loading destination..." />;
  if (!destination || !summary) return <ErrorState message={error} onRetry={load} />;
  const ownReview = summary.reviews.find((review) => review.userId === user?.id);

  const toggleFavorite = async () => {
    setBusy(true); setError("");
    try {
      if (favorite) await removeFavorite(destination.id); else await addFavorite(destination.id);
      setFavorite(!favorite);
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Favorite update failed."); }
    finally { setBusy(false); }
  };
  const submitReview = async (event: FormEvent) => {
    event.preventDefault(); setBusy(true); setError("");
    try {
      if (ownReview) await updateReview(ownReview.id, rating, comment);
      else await createReview(destination.id, rating, comment);
      setSummary(await getReviews(destination.id)); setComment("");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Review update failed."); }
    finally { setBusy(false); }
  };

  return <section className="detail-page">
    <Link to="/destinations">← Back to destinations</Link>
    {destination.coverImageUrl
      ? <img className="detail-image" src={destination.coverImageUrl} alt={destination.name} />
      : <div className="detail-image destination-image-placeholder">No image available</div>}
    <p className="eyebrow">{destination.category}</p><h1>{destination.name}</h1>
    <p className="location">{destination.city ? `${destination.city}, ` : ""}{destination.country}</p>
    <p>{destination.description}</p>
    {destination.latitude != null && destination.longitude != null && <p>Coordinates: {destination.latitude}, {destination.longitude}</p>}
    {destination.attributionSummary && <p className="attribution">Sources: {destination.attributionSummary}</p>}
    <DestinationMap destination={destination} />
    {user
      ? <button disabled={busy} onClick={toggleFavorite}>{favorite ? "Remove from favorites" : "Add to favorites"}</button>
      : <p><Link to="/login" state={{ from: `/destinations/${destination.id}` }}>Login to favorite or review this destination.</Link></p>}
    {error && <div className="error-message" role="alert">{error}</div>}
    <section className="reviews-section">
      <h2>Reviews</h2><p>{summary.averageRating.toFixed(1)} / 5 · {summary.reviewCount} reviews</p>
      {user && <form className="review-form" onSubmit={submitReview}>
        <label>Rating<select value={rating} onChange={(e) => setRating(Number(e.target.value))}>{[5,4,3,2,1].map((n) => <option key={n}>{n}</option>)}</select></label>
        <label>Comment<textarea maxLength={2000} value={comment} onChange={(e) => setComment(e.target.value)} /></label>
        <button disabled={busy}>{busy ? "Saving..." : ownReview ? "Update my review" : "Add review"}</button>
      </form>}
      {summary.reviews.length === 0 ? <p>No reviews yet.</p> : summary.reviews.map((review) =>
        <article className="review-card" key={review.id}>
          <strong>{review.rating} / 5</strong><p>{review.comment || "No comment provided."}</p>
          {(review.userId === user?.id || user?.role === "ADMIN") && <button disabled={busy} onClick={async () => {
            if (!window.confirm("Delete this review?")) return;
            setBusy(true);
            try { await deleteReview(review.id); setSummary(await getReviews(destination.id)); }
            catch (reason) { setError(reason instanceof Error ? reason.message : "Delete failed."); }
            finally { setBusy(false); }
          }}>Delete</button>}
        </article>)}
    </section>
  </section>;
}
