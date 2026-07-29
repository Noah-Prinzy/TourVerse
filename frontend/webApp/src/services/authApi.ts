import type {
  ApiMessage, AuthResponse, LoginRequest, RegisterRequest, UpdateProfileRequest, UserProfile,
} from "../models/Auth";
import { apiBaseUrl, sessionManager } from "./session";

async function parseError(response: Response): Promise<never> {
  let message = `Request failed (HTTP ${response.status}).`;
  try {
    const value = (await response.json()) as Partial<ApiMessage>;
    if (value.message?.trim()) message = value.message;
  } catch { /* Use stable fallback. */ }
  throw new Error(message);
}

export async function jsonRequest<T>(path: string, init: RequestInit, authenticated = false): Promise<T> {
  let response: Response;
  try {
    const request = { ...init, headers: { "Content-Type": "application/json", ...init.headers } };
    response = authenticated
      ? await sessionManager.authenticatedFetch(`${apiBaseUrl()}${path}`, request)
      : await fetch(`${apiBaseUrl()}${path}`, request);
  } catch (reason) {
    if (reason instanceof DOMException && reason.name === "AbortError") throw reason;
    throw new Error("Unable to connect to TourVerse. Check your connection and try again.");
  }
  if (!response.ok) return parseError(response);
  return (await response.json()) as T;
}

export async function login(request: LoginRequest) {
  const response = await jsonRequest<AuthResponse>("/api/auth/login", { method: "POST", body: JSON.stringify(request) });
  sessionManager.establish(response);
  return response;
}

export async function register(request: RegisterRequest) {
  const response = await jsonRequest<AuthResponse>("/api/auth/register", { method: "POST", body: JSON.stringify(request) });
  sessionManager.establish(response);
  return response;
}

export async function logout(all = false) {
  const refreshToken = sessionManager.getRefreshToken();
  try {
    if (all) await jsonRequest<ApiMessage>("/api/auth/logout-all", { method: "POST" }, true);
    else if (refreshToken) await jsonRequest<ApiMessage>("/api/auth/logout", {
      method: "POST", body: JSON.stringify({ refreshToken }),
    });
  } finally {
    sessionManager.clear();
  }
}

export const getProfile = () => jsonRequest<UserProfile>("/api/users/me/profile", { method: "GET" }, true);
export const updateProfile = (request: UpdateProfileRequest) => jsonRequest<UserProfile>(
  "/api/users/me/profile", { method: "PUT", body: JSON.stringify(request) }, true,
);
export const updateProfileImage = (profileImageUrl: string | null) => jsonRequest<UserProfile>(
  "/api/users/me/profile/image", { method: "PUT", body: JSON.stringify({ profileImageUrl }) }, true,
);
export const deleteAccount = (password: string) => jsonRequest<ApiMessage>(
  "/api/users/me", { method: "DELETE", body: JSON.stringify({ password }) }, true,
);
