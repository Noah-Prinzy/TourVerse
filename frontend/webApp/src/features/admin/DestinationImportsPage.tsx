import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import type { DestinationCandidate, DestinationImportBatch } from "../../models/DestinationImport";
import type { Category } from "../../models/Community";
import { getCategories } from "../../services/communityApi";
import { destinationImportApi } from "../../services/destinationImportApi";

export function DestinationImportsPage() {
  const [batches, setBatches] = useState<DestinationImportBatch[]>([]);
  const [candidates, setCandidates] = useState<DestinationCandidate[]>([]);
  const [provider, setProvider] = useState<"WIKIDATA" | "OPENTRIPMAP">("WIKIDATA");
  const [countryCode, setCountryCode] = useState("");
  const [search, setSearch] = useState("");
  const [limit, setLimit] = useState(20);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const load = () => Promise.all([destinationImportApi.batches(), destinationImportApi.candidates()])
    .then(([nextBatches, nextCandidates]) => { setBatches(nextBatches); setCandidates(nextCandidates); })
    .catch((reason: Error) => setError(reason.message));
  useEffect(() => { load(); }, []);
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setBusy(true); setError("");
    try {
      await destinationImportApi.search({ provider, countryCode: countryCode.toUpperCase(), search: search || undefined, limit });
      await load();
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Import failed."); }
    finally { setBusy(false); }
  };
  return <section className="trips-page">
    <p className="eyebrow">Administrator catalogue tools</p>
    <h1>Destination imports</h1>
    <p>External results are unverified candidates. They never appear publicly until explicitly reviewed and approved.</p>
    <form className="form-card compact-form" onSubmit={submit}>
      <label>Provider<select value={provider} onChange={(e) => setProvider(e.target.value as typeof provider)}>
        <option value="WIKIDATA">Wikidata</option><option value="OPENTRIPMAP">OpenTripMap (requires backend key and policy review)</option>
      </select></label>
      <label>ISO country code<input value={countryCode} maxLength={2} required onChange={(e) => setCountryCode(e.target.value)} placeholder="UG" /></label>
      <label>Optional search<input value={search} onChange={(e) => setSearch(e.target.value)} /></label>
      <label>Result limit<input type="number" min={1} max={100} value={limit} onChange={(e) => setLimit(Number(e.target.value))} /></label>
      <button disabled={busy}>{busy ? "Requesting..." : "Start review import"}</button>
    </form>
    {error && <div className="error-message" role="alert"><p>{error}</p><button onClick={load}>Try again</button></div>}
    <h2>Import batches</h2>
    {batches.length === 0 ? <p>No import batches yet.</p> : batches.map((batch) =>
      <article className="trip-card" key={batch.id}><strong>{batch.provider} · {batch.countryCode}</strong>
        <p>{batch.status} · {batch.retrievedCount} retrieved</p>{batch.errorMessage && <p>{batch.errorMessage}</p>}</article>)}
    <h2>Review candidates</h2>
    {candidates.length === 0 ? <p>No candidates await review.</p> : candidates.map((candidate) =>
      <article className="trip-card" key={candidate.id}>
        <strong>{candidate.name}</strong><p>{candidate.country} · {candidate.reviewStatus}</p>
        {candidate.duplicateOfDestinationId && <p role="alert">Possible duplicate: {candidate.duplicateOfDestinationId}</p>}
        <Link to={`/admin/destination-imports/candidates/${candidate.id}`}>Review candidate</Link>
      </article>)}
  </section>;
}

export function DestinationCandidatePage() {
  const { candidateId } = useParams();
  const [candidate, setCandidate] = useState<DestinationCandidate | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState("");
  const load = () => candidateId && Promise.all([destinationImportApi.candidate(candidateId), getCategories()])
    .then(([item, nextCategories]) => { setCandidate(item); setCategories(nextCategories); })
    .catch((reason: Error) => setError(reason.message));
  useEffect(() => { load(); }, [candidateId]);
  if (error && !candidate) return <section className="trips-page"><div className="error-message" role="alert">{error}</div></section>;
  if (!candidate) return <section className="trips-page"><p>Loading candidate...</p></section>;
  const updateCategory = async (value: string) => setCandidate(await destinationImportApi.update(candidate.id, { mappedCategory: value }));
  const save = async () => {
    try {
      setCandidate(await destinationImportApi.update(candidate.id, {
        name: candidate.name, countryCode: candidate.countryCode,
        country: candidate.country, city: candidate.city,
        descriptionHint: candidate.descriptionHint,
        mappedCategory: candidate.mappedCategory,
      }));
      setError("");
    } catch (reason) { setError((reason as Error).message); }
  };
  const approve = async () => {
    if (!window.confirm(`Approve ${candidate.name} as a public TourVerse destination?`)) return;
    try { setCandidate(await destinationImportApi.approve(candidate.id)); }
    catch (reason) { await load(); setError((reason as Error).message); }
  };
  const reject = async () => {
    const reason = window.prompt("Reason for rejection?"); if (!reason) return;
    setCandidate(await destinationImportApi.reject(candidate.id, reason));
  };
  const link = async () => {
    const destinationId = window.prompt("Existing TourVerse destination UUID to link?");
    if (!destinationId) return;
    try { setCandidate(await destinationImportApi.link(candidate.id, destinationId)); }
    catch (reason) { setError((reason as Error).message); }
  };
  return <section className="trips-page">
    <Link to="/admin/destination-imports">Back to imports</Link><h1>{candidate.name}</h1>
    {error && <div className="error-message" role="alert">{error}</div>}
    <p><strong>Source:</strong> {candidate.sourceProvider} / {candidate.externalId}</p>
    {candidate.sourceUrl && <p><a href={candidate.sourceUrl} target="_blank" rel="noreferrer">View source record</a></p>}
    <p><strong>Status:</strong> {candidate.reviewStatus}</p>
    <div className="form-card compact-form">
      <label>Name<input value={candidate.name} onChange={(e) => setCandidate({ ...candidate, name: e.target.value })} /></label>
      <label>ISO country code<input maxLength={2} value={candidate.countryCode ?? ""} onChange={(e) => setCandidate({ ...candidate, countryCode: e.target.value.toUpperCase() })} /></label>
      <label>Country<input value={candidate.country} onChange={(e) => setCandidate({ ...candidate, country: e.target.value })} /></label>
      <label>City (optional)<input value={candidate.city ?? ""} onChange={(e) => setCandidate({ ...candidate, city: e.target.value || null })} /></label>
      <label>Description hint<textarea value={candidate.descriptionHint ?? ""} onChange={(e) => setCandidate({ ...candidate, descriptionHint: e.target.value || null })} /></label>
      <button onClick={save} disabled={candidate.reviewStatus === "APPROVED"}>Save review edits</button>
    </div>
    <label>TourVerse category<select value={candidate.mappedCategory ?? ""} onChange={(e) => updateCategory(e.target.value)}>
      <option value="">Requires review</option>{categories.map((item) => <option key={item.id} value={item.name}>{item.name}</option>)}
    </select></label>
    {candidate.imageReference && <p>Image must not be approved without verified licence and attribution.</p>}
    {candidate.duplicateOfDestinationId && <p role="alert">Possible duplicate of {candidate.duplicateOfDestinationId}. Link or reject it instead of approving.</p>}
    <p><button onClick={approve} disabled={candidate.reviewStatus === "APPROVED"}>Approve destination</button>{" "}
      <button className="danger-button" onClick={reject} disabled={candidate.reviewStatus === "APPROVED"}>Reject</button></p>
    {candidate.reviewStatus !== "APPROVED" && <button className="secondary-button" onClick={link}>Link existing destination</button>}
  </section>;
}
