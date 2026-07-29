import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import type { UserProfile } from "../../models/Auth";
import { deleteAccount, getProfile, updateProfile, updateProfileImage } from "../../services/authApi";
import { sessionManager } from "../../services/session";
import { ErrorState, LoadingState } from "../../components/StatusStates";
import { useAuth } from "../../state/AuthContext";

export function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [deletePassword, setDeletePassword] = useState("");
  const { logout } = useAuth();
  const navigate = useNavigate();
  const load = () => { setError(""); getProfile().then(setProfile).catch((reason) => setError(reason.message)); };
  useEffect(load, []);
  if (!profile && !error) return <LoadingState label="Loading profile..." />;
  if (!profile) return <ErrorState message={error} onRetry={load} />;

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setSaving(true); setError("");
    try {
      const data = new FormData(event.currentTarget);
      await updateProfile({
        firstName: String(data.get("firstName") ?? ""), lastName: String(data.get("lastName") ?? ""),
        bio: String(data.get("bio") ?? ""), nationality: String(data.get("nationality") ?? ""),
        travelInterests: String(data.get("travelInterests") ?? "").split(",").map((v) => v.trim()).filter(Boolean),
        profilePublic: data.get("profilePublic") === "on",
      });
      const image = String(data.get("profileImageUrl") ?? "").trim();
      setProfile(await updateProfileImage(image || null));
      await sessionManager.refresh();
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to update profile."); }
    finally { setSaving(false); }
  };

  const remove = async () => {
    if (!deletePassword || !window.confirm("Permanently delete your TourVerse account?")) return;
    setSaving(true);
    try { await deleteAccount(deletePassword); sessionManager.clear(); navigate("/register", { replace: true }); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Unable to delete account."); }
    finally { setSaving(false); }
  };

  return <section className="form-page"><form className="form-card" onSubmit={save}>
    <h1>Your profile</h1>{error && <div className="error-message" role="alert">{error}</div>}
    <label>First name<input name="firstName" required defaultValue={profile.firstName} /></label>
    <label>Last name<input name="lastName" required defaultValue={profile.lastName} /></label>
    <label>Email<input value={profile.email} disabled /></label>
    <label>Bio<textarea name="bio" defaultValue={profile.bio ?? ""} /></label>
    <label>Nationality<input name="nationality" defaultValue={profile.nationality ?? ""} /></label>
    <label>Travel interests<input name="travelInterests" defaultValue={profile.travelInterests.join(", ")} /></label>
    <label>Profile image URL<input type="url" name="profileImageUrl" defaultValue={profile.profileImageUrl ?? ""} /></label>
    <label className="checkbox-row"><input type="checkbox" name="profilePublic" defaultChecked={profile.profilePublic} /> Public profile</label>
    <button disabled={saving}>{saving ? "Saving..." : "Save profile"}</button>
    <button type="button" className="secondary-button" onClick={() => logout(true).finally(() => navigate("/login"))}>Log out all sessions</button>
    <hr /><h2>Delete account</h2>
    <label>Confirm password<input type="password" value={deletePassword} onChange={(e) => setDeletePassword(e.target.value)} /></label>
    <button type="button" className="danger-button" disabled={saving || !deletePassword} onClick={remove}>Delete account permanently</button>
  </form></section>;
}
