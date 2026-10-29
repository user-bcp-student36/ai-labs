import '@angular/compiler';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import axios from 'axios';
import { ChangeDetectorRef, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { installLocalStorageStub } from '../../testing/local-storage.stub';

vi.mock('axios', () => ({ default: { get: vi.fn(), post: vi.fn() } }));

const mockedGet = vi.mocked(axios.get);
const TASKS_URL = 'http://localhost:8000/api/tasks?owner=ana';
const TOKEN = 'tok-123';

const TASKS = [
  { title: 'Tarea A', description: 'a', status: 'TODO', priority: 'high', owner: 'ana' },
  { title: 'Tarea B', description: 'b', status: 'ARCHIVED', priority: 'low', owner: 'ana' },
  { title: 'Tarea C', description: 'c', status: 'IN_PROGRESS', priority: 'normal', owner: 'ana' }
];

function createDashboard() {
  const navigate = vi.fn().mockResolvedValue(true);
  const detectChanges = vi.fn();
  const zone = { run: (fn: () => void) => fn() };
  const component = new DashboardComponent(
    { navigate } as unknown as Router,
    { detectChanges } as unknown as ChangeDetectorRef,
    zone as unknown as NgZone
  );
  return { component, navigate, detectChanges };
}

describe('DashboardComponent', () => {
  let storage: Storage;

  beforeEach(() => {
    storage = installLocalStorageStub();
    storage.setItem('workshop_token', TOKEN);
    storage.setItem('workshop_user', JSON.stringify({ username: 'ana' }));
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  describe('camino feliz', () => {
    it('carga las tareas del usuario con el token, oculta las ARCHIVED y etiqueta las visibles', async () => {
      mockedGet.mockResolvedValueOnce({ data: TASKS } as never);
      const { component, detectChanges } = createDashboard();

      await component.ngOnInit();

      expect(mockedGet).toHaveBeenCalledWith(TASKS_URL, { headers: { Authorization: 'Bearer ' + TOKEN } });
      const titles = component.visibleTasks.map((task) => task.title).sort();
      expect(titles).toEqual(['Tarea A', 'Tarea C']);
      const taskA = component.visibleTasks.find((task) => task.title === 'Tarea A');
      expect(taskA?.label).toBe('HIGH · ANA');
      expect(component.loading).toBe(false);
      expect(component.error).toBe('');
      expect(detectChanges).toHaveBeenCalled();
    });

    it('traduce el estado conocido a su etiqueta en español', () => {
      const { component } = createDashboard();

      expect(component.statusLabel('todo')).toBe('Por hacer');
      expect(component.statusLabel('IN_PROGRESS')).toBe('En progreso');
    });

    it('cierra sesión, limpia el almacenamiento y navega a /login', async () => {
      const { component, navigate } = createDashboard();

      await component.logout();

      expect(storage.getItem('workshop_token')).toBeNull();
      expect(storage.getItem('workshop_user')).toBeNull();
      expect(navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('casos borde', () => {
    it('redirige a /login sin llamar a la API si no hay token', async () => {
      storage.removeItem('workshop_token');
      const { component, navigate } = createDashboard();

      await component.ngOnInit();

      expect(navigate).toHaveBeenCalledWith(['/login']);
      expect(mockedGet).not.toHaveBeenCalled();
    });

    it('muestra la lista vacía cuando el backend no devuelve tareas', async () => {
      mockedGet.mockResolvedValueOnce({ data: [] } as never);
      const { component } = createDashboard();

      await component.ngOnInit();

      expect(component.visibleTasks).toEqual([]);
      expect(component.loading).toBe(false);
      expect(component.error).toBe('');
    });

    it('deja la lista visible vacía cuando todas las tareas están ARCHIVED', async () => {
      mockedGet.mockResolvedValueOnce({ data: [TASKS[1]] } as never);
      const { component } = createDashboard();

      await component.ngOnInit();

      expect(component.visibleTasks).toEqual([]);
      expect(component.error).toBe('');
    });

    it('devuelve el estado original si no existe etiqueta conocida', () => {
      const { component } = createDashboard();

      expect(component.statusLabel('BLOQUEADA')).toBe('BLOQUEADA');
      expect(component.statusLabel('')).toBe('');
    });
  });

  describe('errores', () => {
    it('ante HTTP 401 borra la sesión y redirige a /login con aviso de expiración sin exponer el token', async () => {
      const consoleSpies = [
        vi.spyOn(console, 'error').mockImplementation(() => undefined),
        vi.spyOn(console, 'log').mockImplementation(() => undefined),
        vi.spyOn(console, 'warn').mockImplementation(() => undefined)
      ];
      storage.setItem('workshop_preferences', JSON.stringify({ theme: 'dark' }));
      mockedGet.mockRejectedValueOnce({
        message: 'Request failed with status code 401',
        response: { status: 401, data: { detail: 'Token inválido' } }
      });
      const { component, navigate } = createDashboard();

      await component.ngOnInit();

      expect(storage.getItem('workshop_token')).toBeNull();
      expect(storage.getItem('workshop_user')).toBeNull();
      expect(storage.getItem('workshop_preferences')).toBeNull();
      expect(navigate).toHaveBeenCalledWith(['/login'], { state: { sessionExpired: true } });
      for (const spy of consoleSpies) {
        expect(JSON.stringify(spy.mock.calls)).not.toContain(TOKEN);
      }
    });

    it('muestra el error de conexión cuando el servicio de tareas no está disponible', async () => {
      mockedGet.mockRejectedValueOnce(new Error('Network Error'));
      const { component } = createDashboard();

      await component.ngOnInit();

      expect(component.error).toBe('Network Error');
      expect(component.loading).toBe(false);
    });

    it('muestra el error de timeout recibido de axios', async () => {
      mockedGet.mockRejectedValueOnce(
        Object.assign(new Error('timeout of 5000ms exceeded'), { code: 'ECONNABORTED' })
      );
      const { component } = createDashboard();

      await component.ngOnInit();

      expect(component.error).toBe('timeout of 5000ms exceeded');
      expect(component.loading).toBe(false);
    });

    it('ante HTTP 500 muestra el error y conserva el token y los datos de sesión', async () => {
      mockedGet.mockRejectedValueOnce({
        message: 'Request failed with status code 500',
        response: { status: 500, data: { detail: 'Error interno' } }
      });
      const { component, navigate } = createDashboard();

      await component.ngOnInit();

      expect(component.error).toBe('Error interno');
      expect(component.loading).toBe(false);
      expect(storage.getItem('workshop_token')).toBe(TOKEN);
      expect(storage.getItem('workshop_user')).not.toBeNull();
      expect(navigate).not.toHaveBeenCalled();
    });

    it('ante error de red no expone el token en la interfaz ni en la consola', async () => {
      const consoleSpies = [
        vi.spyOn(console, 'error').mockImplementation(() => undefined),
        vi.spyOn(console, 'log').mockImplementation(() => undefined),
        vi.spyOn(console, 'warn').mockImplementation(() => undefined)
      ];
      mockedGet.mockRejectedValueOnce(new Error('Network Error'));
      const { component } = createDashboard();

      await component.ngOnInit();

      expect(component.error).toBe('Network Error');
      expect(component.error).not.toContain(TOKEN);
      for (const spy of consoleSpies) {
        expect(JSON.stringify(spy.mock.calls)).not.toContain(TOKEN);
      }
    });
  });
});
