/**
 * JWT Auth utility – decode token, extract user email and roles.
 * No external library needed; uses base64 decoding.
 */

export interface JwtPayload {
  sub: string; // email
  roles: string[];
  iat: number;
  exp: number;
}

/**
 * Decode a JWT token (base64) and return the payload.
 * Returns null if the token is missing or malformed.
 */
export function decodeJwt(): JwtPayload | null {
  const token = localStorage.getItem("jwt-token");
  if (!token) return null;

  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = JSON.parse(atob(parts[1]));
    return payload as JwtPayload;
  } catch {
    console.error("Failed to decode JWT token");
    return null;
  }
}

/** Get the logged-in user's email from the JWT subject claim. */
export function getUserEmail(): string | null {
  const payload = decodeJwt();
  return payload?.sub ?? null;
}

/** Get the logged-in user's roles array from the JWT. */
export function getUserRoles(): string[] {
  const payload = decodeJwt();
  return payload?.roles ?? [];
}

/** Check whether the current user has the ADMIN or SUPERADMIN role. */
export function isAdmin(): boolean {
  const roles = getUserRoles();
  return roles.includes("ADMIN") || roles.includes("SUPERADMIN");
}

/** Check whether the JWT token is expired. */
export function isTokenExpired(): boolean {
  const payload = decodeJwt();
  if (!payload) return true;
  return Date.now() >= payload.exp * 1000;
}
