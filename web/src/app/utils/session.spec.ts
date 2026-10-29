import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  PREFERENCES_KEY,
  TOKEN_KEY,
  USER_KEY,
  clearSession,
  expireSession,
  readPreferencesRaw,
  readToken,
  readUser,
  saveSession,
  savePreferences
} from './session';
import { installLocalStorageStub } from '../../testing/local-storage.stub';

describe('session', () => {
  let storage: Storage;

  beforeEach(() => {
    storage = installLocalStorageStub();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('mantiene las claves de localStorage del contrato', () => {
    expect(TOKEN_KEY).toBe('workshop_token');
    expect(USER_KEY).toBe('workshop_user');
    expect(PREFERENCES_KEY).toBe('workshop_preferences');
  });

  it('guarda el token exacto y el usuario serializado', () => {
    saveSession('tok-123', { username: 'ana' });

    expect(storage.getItem('workshop_token')).toBe('tok-123');
    expect(JSON.parse(storage.getItem('workshop_user') as string)).toEqual({ username: 'ana' });
  });

  it('lee el token y el usuario guardados', () => {
    saveSession('tok-123', { username: 'ana' });

    expect(readToken()).toBe('tok-123');
    expect(readUser()).toEqual({ username: 'ana' });
  });

  it('devuelve null y un objeto vacío cuando no hay sesión', () => {
    expect(readToken()).toBeNull();
    expect(readUser()).toEqual({});
  });

  it('borra token y usuario sin tocar las preferencias', () => {
    saveSession('tok-123', { username: 'ana' });
    savePreferences({ theme: 'dark' });

    clearSession();

    expect(storage.getItem('workshop_token')).toBeNull();
    expect(storage.getItem('workshop_user')).toBeNull();
    expect(readPreferencesRaw()).toBe(JSON.stringify({ theme: 'dark' }));
  });

  it('expira la sesión borrando token, usuario y preferencias', () => {
    saveSession('tok-123', { username: 'ana' });
    savePreferences({ theme: 'dark' });

    expireSession();

    expect(storage.getItem('workshop_token')).toBeNull();
    expect(storage.getItem('workshop_user')).toBeNull();
    expect(storage.getItem('workshop_preferences')).toBeNull();
  });

  it('guarda y lee las preferencias como texto JSON', () => {
    savePreferences({ compact: true });

    expect(readPreferencesRaw()).toBe('{"compact":true}');
  });
});
