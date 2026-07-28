import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { User } from "../models/Auth";
import { logout as logoutRequest } from "../services/authApi";
import { sessionManager } from "../services/session";

interface AuthState {
  user: User | null;
  initializing: boolean;
  logout: (all?: boolean) => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState(sessionManager.user);
  const [initializing, setInitializing] = useState(sessionManager.hasPersistedSession);
  useEffect(() => sessionManager.subscribe(setUser), []);
  useEffect(() => { sessionManager.restore().finally(() => setInitializing(false)); }, []);
  const value = useMemo(() => ({
    user, initializing, logout: (all = false) => logoutRequest(all),
  }), [user, initializing]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider.");
  return value;
}
