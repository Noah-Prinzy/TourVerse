import { beforeEach, describe, expect, it, vi } from "vitest";

const values = new Map<string, string>();
const localStorage = {
  getItem: (key: string) => values.get(key) ?? null,
  setItem: (key: string, value: string) => values.set(key, value),
  removeItem: (key: string) => values.delete(key),
};

const auth = {
  accessToken: "access-one",
  refreshToken: "refresh-one",
  tokenType: "Bearer",
  expiresInSeconds: 3600,
  user: {
    id: "11111111-1111-4111-8111-111111111111",
    firstName: "Test",
    lastName: "User",
    email: "test@example.com",
    profileImageUrl: null,
    bio: null,
    role: "USER",
    createdAt: "2026-01-01T00:00:00Z",
  },
};

describe("SessionManager", () => {
  beforeEach(() => {
    values.clear();
    vi.resetModules();
    vi.stubGlobal("window", { localStorage });
  });

  it("attaches the bearer access token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("{}", { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const { sessionManager } = await import("./session");
    sessionManager.establish(auth);
    await sessionManager.authenticatedFetch("https://example.test/protected");
    const headers = new Headers(fetchMock.mock.calls[0][1].headers);
    expect(headers.get("Authorization")).toBe("Bearer access-one");
  });

  it("uses one refresh operation for concurrent unauthorized requests", async () => {
    let protectedCalls = 0;
    let refreshCalls = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      if (String(input).endsWith("/api/auth/refresh")) {
        refreshCalls++;
        return new Response(JSON.stringify({ ...auth, accessToken: "access-two", refreshToken: "refresh-two" }), {
          status: 200, headers: { "Content-Type": "application/json" },
        });
      }
      protectedCalls++;
      return new Response("{}", { status: protectedCalls <= 2 ? 401 : 200 });
    });
    vi.stubGlobal("fetch", fetchMock);
    const { sessionManager } = await import("./session");
    sessionManager.establish(auth);
    await Promise.all([
      sessionManager.authenticatedFetch("https://example.test/one"),
      sessionManager.authenticatedFetch("https://example.test/two"),
    ]);
    expect(refreshCalls).toBe(1);
  });

  it("clears persisted session material when refresh fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("{}", { status: 401 })));
    values.set("tourverse.refreshToken", "expired");
    const { sessionManager } = await import("./session");
    expect(await sessionManager.restore()).toBe(false);
    expect(values.has("tourverse.refreshToken")).toBe(false);
  });
});
