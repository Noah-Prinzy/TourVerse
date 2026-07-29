import type { AuthResponse, User } from "../models/Auth";

const REFRESH_TOKEN_KEY = "tourverse.refreshToken";
type SessionListener = (user: User | null) => void;

export function apiBaseUrl(): string {
  return (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081").replace(/\/+$/, "");
}

class SessionManager {
  private accessToken: string | null = null;
  private refreshToken: string | null = window.localStorage.getItem(REFRESH_TOKEN_KEY);
  private currentUser: User | null = null;
  private refreshPromise: Promise<boolean> | null = null;
  private listeners = new Set<SessionListener>();

  get user() { return this.currentUser; }
  get hasPersistedSession() { return Boolean(this.refreshToken); }
  getRefreshToken() { return this.refreshToken; }

  subscribe(listener: SessionListener) {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  establish(response: AuthResponse) {
    this.accessToken = response.accessToken;
    this.refreshToken = response.refreshToken;
    this.currentUser = response.user;
    window.localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    this.notify();
  }

  clear() {
    this.accessToken = null;
    this.refreshToken = null;
    this.currentUser = null;
    window.localStorage.removeItem(REFRESH_TOKEN_KEY);
    this.notify();
  }

  async restore() {
    return this.refreshToken ? this.refresh() : false;
  }

  async authenticatedFetch(input: RequestInfo | URL, init: RequestInit = {}) {
    const response = await fetch(input, this.withBearer(init));
    if (response.status !== 401 || init.signal?.aborted || !(await this.refresh())) return response;
    return fetch(input, this.withBearer(init));
  }

  async refresh(): Promise<boolean> {
    if (!this.refreshToken) return false;
    if (!this.refreshPromise) {
      this.refreshPromise = this.performRefresh().finally(() => { this.refreshPromise = null; });
    }
    return this.refreshPromise;
  }

  private async performRefresh() {
    try {
      const response = await fetch(`${apiBaseUrl()}/api/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: this.refreshToken }),
      });
      if (!response.ok) {
        this.clear();
        return false;
      }
      this.establish((await response.json()) as AuthResponse);
      return true;
    } catch {
      this.clear();
      return false;
    }
  }

  private withBearer(init: RequestInit): RequestInit {
    const headers = new Headers(init.headers);
    if (this.accessToken) headers.set("Authorization", `Bearer ${this.accessToken}`);
    return { ...init, headers };
  }

  private notify() { this.listeners.forEach((listener) => listener(this.currentUser)); }
}

export const sessionManager = new SessionManager();
