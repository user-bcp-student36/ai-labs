export const TOKEN_KEY = 'workshop_token';
export const USER_KEY = 'workshop_user';
export const PREFERENCES_KEY = 'workshop_preferences';

export function saveSession(token: string, user: unknown): void {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function readToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function readUser(): any {
  return JSON.parse(localStorage.getItem(USER_KEY) || '{}');
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function expireSession(): void {
  clearSession();
  localStorage.removeItem(PREFERENCES_KEY);
}

export function readPreferencesRaw(): string | null {
  return localStorage.getItem(PREFERENCES_KEY);
}

export function savePreferences(preferences: unknown): void {
  localStorage.setItem(PREFERENCES_KEY, JSON.stringify(preferences));
}
