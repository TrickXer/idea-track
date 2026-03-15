import React, { createContext, useContext, useMemo, useState } from "react";
import restApi from "./restApi";
import { setToken as saveToken, clearToken, getToken } from "./storage";

// ─── JWT decode ──────────────────────────────────────────────────
export interface JwtPayload {
  sub: string;       // email
  roles: string[];
  iat: number;
  exp: number;
}

export function decodeJwt(token?: string | null): JwtPayload | null {
  if (!token) return null;
  try {
    const base64 = token.split(".")[1];
    const json = atob(base64.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

// ─── Context type ────────────────────────────────────────────────
type AuthState = {
  token: string | null;
  payload: JwtPayload | null;
  /** Roles as uppercase strings, e.g. ["ADMIN"] */
  roles: string[];
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthState | null>(null);

// ─── Provider ────────────────────────────────────────────────────
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(getToken());
  const payload = useMemo(() => decodeJwt(token), [token]);
  const roles = useMemo(
    () => (payload?.roles ?? []).map((r) => String(r).toUpperCase()),
    [payload]
  );

  const login = async (email: string, password: string) => {
    const res = await restApi.post<{ token: string; message: string }>("/api/auth/login", {
      email,
      password,
    });
    const { token: newToken } = res.data;
    saveToken(newToken);
    setToken(newToken);
  };

  const logout = () => {
    clearToken();
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ token, payload, roles, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// ─── Hook ────────────────────────────────────────────────────────
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
