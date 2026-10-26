import { vi } from 'vitest';

/**
 * Reemplaza el global `localStorage` (no disponible en el entorno `node` de Vitest)
 * por un almacen en memoria. Debe llamarse dentro de beforeEach y se limpia con
 * vi.unstubAllGlobals() en afterEach.
 */
export function installLocalStorageStub(): Storage {
  const store = new Map<string, string>();
  const storage = {
    get length(): number {
      return store.size;
    },
    key: (index: number): string | null => Array.from(store.keys())[index] ?? null,
    getItem: (key: string): string | null => (store.has(key) ? (store.get(key) as string) : null),
    setItem: (key: string, value: string): void => {
      store.set(key, String(value));
    },
    removeItem: (key: string): void => {
      store.delete(key);
    },
    clear: (): void => {
      store.clear();
    }
  } as unknown as Storage;

  vi.stubGlobal('localStorage', storage);
  return storage;
}
