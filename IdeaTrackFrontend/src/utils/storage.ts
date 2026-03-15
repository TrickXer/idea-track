// Token storage helpers – single key used across all modules
const TOKEN_KEY = "jwt-token";

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function getToken(): string | null {
  // Support legacy keys used by older modules during migration
  return (
    localStorage.getItem(TOKEN_KEY) ||
    localStorage.getItem("JWT-Token") ||
    localStorage.getItem("jwtToken") ||
    null
  );
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem("JWT-Token");
  localStorage.removeItem("jwtToken");
}
