import { useRef, useState, type FormEvent, type ReactNode, type RefObject } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { login, register } from "../../services/authApi";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const errorRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const location = useLocation();
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setSubmitting(true); setError("");
    try {
      await login({ email, password });
      navigate((location.state as { from?: string } | null)?.from ?? "/destinations", { replace: true });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Login failed.");
      requestAnimationFrame(() => errorRef.current?.focus());
    } finally { setSubmitting(false); }
  };
  return <AuthForm title="Welcome back" onSubmit={submit} error={error} errorRef={errorRef}>
    <label>Email<input type="email" required autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} /></label>
    <label>Password<input type={showPassword ? "text" : "password"} required autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} /></label>
    <label className="checkbox-row"><input type="checkbox" checked={showPassword} onChange={(e) => setShowPassword(e.target.checked)} /> Show password</label>
    <button disabled={submitting}>{submitting ? "Signing in..." : "Login"}</button>
    <p>New to TourVerse? <Link to="/register">Create an account</Link>.</p>
  </AuthForm>;
}

export function RegisterPage() {
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const errorRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setSubmitting(true); setError("");
    try { await register(form); navigate("/profile", { replace: true }); }
    catch (reason) {
      setError(reason instanceof Error ? reason.message : "Registration failed.");
      requestAnimationFrame(() => errorRef.current?.focus());
    } finally { setSubmitting(false); }
  };
  return <AuthForm title="Create your account" onSubmit={submit} error={error} errorRef={errorRef}>
    <label>First name<input required value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} /></label>
    <label>Last name<input required value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} /></label>
    <label>Email<input type="email" required autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
    <label>Password<input type="password" minLength={8} required autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
    <p className="field-help">Use at least 8 characters with uppercase, lowercase and a number.</p>
    <button disabled={submitting}>{submitting ? "Creating account..." : "Register"}</button>
  </AuthForm>;
}

function AuthForm({ title, onSubmit, error, errorRef, children }: {
  title: string; onSubmit: (event: FormEvent) => void; error: string;
  errorRef: RefObject<HTMLDivElement | null>; children: ReactNode;
}) {
  return <section className="form-page"><form className="form-card" onSubmit={onSubmit}>
    <h1>{title}</h1>
    {error && <div className="error-message" role="alert" tabIndex={-1} ref={errorRef}>{error}</div>}
    {children}
  </form></section>;
}
