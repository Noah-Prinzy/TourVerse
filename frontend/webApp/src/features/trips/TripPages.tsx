import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/StatusStates";
import type { Trip } from "../../models/Community";
import {
  addTripDestination, createTrip, deleteTrip, getTrip, getTrips, isUuid,
  removeTripDestination, updateTrip,
} from "../../services/communityApi";

export function TripsPage() {
  const [items, setItems] = useState<Trip[] | null>(null);
  const [error, setError] = useState(""); const [busy, setBusy] = useState(false);
  const [title, setTitle] = useState(""); const [description, setDescription] = useState("");
  const [startDate, setStartDate] = useState(""); const [endDate, setEndDate] = useState("");
  const navigate = useNavigate();
  const load = () => { setError(""); getTrips().then(setItems).catch((e) => setError(e.message)); };
  useEffect(load, []);
  const submit = async (event: FormEvent) => {
    event.preventDefault(); if (busy) return; setBusy(true); setError("");
    try {
      const trip = await createTrip({ title, description: description || undefined, startDate: startDate || null, endDate: endDate || null });
      navigate(`/trips/${trip.id}`);
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to create trip."); }
    finally { setBusy(false); }
  };
  if (!items && !error) return <LoadingState label="Loading trips..." />;
  if (!items) return <ErrorState message={error} onRetry={load} />;
  return <section className="trips-page"><h1>My trips</h1>{error && <div className="error-message">{error}</div>}
    <form className="form-card compact-form" onSubmit={submit}>
      <h2>Create a trip</h2><label>Title<input required maxLength={150} value={title} onChange={(e) => setTitle(e.target.value)} /></label>
      <label>Description<textarea value={description} onChange={(e) => setDescription(e.target.value)} /></label>
      <label>Start date<input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} /></label>
      <label>End date<input type="date" min={startDate || undefined} value={endDate} onChange={(e) => setEndDate(e.target.value)} /></label>
      <button disabled={busy}>{busy ? "Creating..." : "Create trip"}</button>
    </form>
    {items.length === 0 ? <EmptyState>No trips yet.</EmptyState> : <div className="trip-grid">{items.map((trip) =>
      <article className="trip-card" key={trip.id}><h2>{trip.title}</h2><p>{trip.description || "No description"}</p><p>{trip.destinations.length} destinations</p><Link to={`/trips/${trip.id}`}>Open trip</Link></article>)}</div>}
  </section>;
}

export function TripDetailsPage() {
  const { tripId } = useParams(); const navigate = useNavigate();
  const [trip, setTrip] = useState<Trip | null>(null); const [error, setError] = useState("");
  const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false);
  const [destinationId, setDestinationId] = useState(""); const [visitDate, setVisitDate] = useState(""); const [notes, setNotes] = useState("");
  const load = async () => {
    if (!isUuid(tripId)) { setError("The trip ID is invalid."); setLoading(false); return; }
    setLoading(true); setError("");
    try { setTrip(await getTrip(tripId)); } catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to load trip."); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, [tripId]);
  if (loading) return <LoadingState label="Loading trip..." />;
  if (!trip || !isUuid(tripId)) return <ErrorState message={error} onRetry={load} />;
  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setBusy(true);
    const data = new FormData(event.currentTarget);
    try { setTrip(await updateTrip(trip.id, {
      title: String(data.get("title")), description: String(data.get("description")),
      startDate: String(data.get("startDate")) || null, endDate: String(data.get("endDate")) || null,
    })); } catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to update trip."); }
    finally { setBusy(false); }
  };
  return <section className="trips-page"><Link to="/trips">← My trips</Link>{error && <div className="error-message">{error}</div>}
    <form className="form-card compact-form" onSubmit={save}><h1>Trip details</h1>
      <label>Title<input name="title" required defaultValue={trip.title} /></label>
      <label>Description<textarea name="description" defaultValue={trip.description ?? ""} /></label>
      <label>Start date<input name="startDate" type="date" defaultValue={trip.startDate ?? ""} /></label>
      <label>End date<input name="endDate" type="date" defaultValue={trip.endDate ?? ""} /></label>
      <button disabled={busy}>Save trip</button>
    </form>
    <form className="form-card compact-form" onSubmit={async (event) => {
      event.preventDefault(); if (!isUuid(destinationId)) { setError("Enter a valid destination UUID."); return; }
      setBusy(true); try { setTrip(await addTripDestination(trip.id, destinationId, visitDate, notes, trip.destinations.length)); setDestinationId(""); setVisitDate(""); setNotes(""); }
      catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to add destination."); } finally { setBusy(false); }
    }}><h2>Add destination</h2>
      <label>Destination UUID<input required value={destinationId} onChange={(e) => setDestinationId(e.target.value)} /></label>
      <label>Visit date<input type="date" value={visitDate} onChange={(e) => setVisitDate(e.target.value)} /></label>
      <label>Notes<textarea value={notes} onChange={(e) => setNotes(e.target.value)} /></label><button disabled={busy}>Add destination</button>
    </form>
    <h2>Itinerary</h2>{trip.destinations.length === 0 ? <EmptyState>No destinations in this trip.</EmptyState> :
      <ol className="itinerary">{trip.destinations.map((entry) => <li key={entry.id}><Link to={`/destinations/${entry.destination.id}`}>{entry.destination.name}</Link>
        <p>{entry.visitDate || "Date not set"}{entry.notes ? ` · ${entry.notes}` : ""}</p>
        <button disabled={busy} onClick={async () => { setBusy(true); try { setTrip(await removeTripDestination(trip.id, entry.destination.id)); } catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to remove destination."); } finally { setBusy(false); } }}>Remove</button>
      </li>)}</ol>}
    <button className="danger-button" disabled={busy} onClick={async () => {
      if (!window.confirm("Delete this trip permanently?")) return;
      setBusy(true); try { await deleteTrip(trip.id); navigate("/trips"); } catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to delete trip."); } finally { setBusy(false); }
    }}>Delete trip</button>
  </section>;
}
