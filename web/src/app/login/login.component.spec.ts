import '@angular/compiler';
import { readFileSync } from 'node:fs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import axios from 'axios';
import { Router } from '@angular/router';
import { LoginComponent } from './login.component';
import { installLocalStorageStub } from '../../testing/local-storage.stub';

vi.mock('axios', () => ({ default: { get: vi.fn(), post: vi.fn() } }));

const mockedPost = vi.mocked(axios.post);
const LOGIN_URL = 'http://localhost:8080/auth/login';
const VALID_PASSWORD = 'secreto-de-prueba';

function createLogin(username: string, password: string) {
  const navigate = vi.fn().mockResolvedValue(true);
  const component = new LoginComponent({ navigate } as unknown as Router);
  component.username = username;
  component.password = password;
  return { component, navigate };
}

describe('LoginComponent', () => {
  let storage: Storage;

  beforeEach(() => {
    storage = installLocalStorageStub();
    vi.clearAllMocks();
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(console, 'log').mockImplementation(() => undefined);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  describe('camino feliz', () => {
    it('inicia sesión con credenciales válidas, guarda la sesión y navega a /dashboard', async () => {
      mockedPost.mockResolvedValueOnce({
        data: { token: 'tok-123', user: { username: 'ana', role: 'student' } }
      } as never);
      const { component, navigate } = createLogin('ana', VALID_PASSWORD);

      await component.login();

      expect(mockedPost).toHaveBeenCalledWith(
        LOGIN_URL,
        { username: 'ana', password: VALID_PASSWORD, client: 'angular-workshop' },
        { timeout: 5000 }
      );
      expect(storage.getItem('workshop_token')).toBe('tok-123');
      expect(JSON.parse(storage.getItem('workshop_user') as string)).toEqual({ username: 'ana', role: 'student' });
      expect(storage.getItem('workshop_preferences')).toBe(JSON.stringify(component.preferences));
      expect(navigate).toHaveBeenCalledWith(['/dashboard']);
      expect(component.message).toBeNull();
      expect(component.busy).toBe(false);
    });
  });

  describe('casos borde', () => {
    it('no llama al backend si el usuario está vacío o solo tiene espacios', async () => {
      const { component, navigate } = createLogin('   ', VALID_PASSWORD);

      await component.login();

      expect(component.message).toEqual({ headline: 'Ingrese un usuario.' });
      expect(mockedPost).not.toHaveBeenCalled();
      expect(navigate).not.toHaveBeenCalled();
    });

    it('no llama al backend si la contraseña está vacía', async () => {
      const { component, navigate } = createLogin('ana', '');

      await component.login();

      expect(component.message).toEqual({ headline: 'Ingrese una contraseña.' });
      expect(mockedPost).not.toHaveBeenCalled();
      expect(navigate).not.toHaveBeenCalled();
    });

    it('muestra mensaje y no guarda sesión si la respuesta 200 no trae token', async () => {
      mockedPost.mockResolvedValueOnce({ data: { user: { username: 'ana' } } } as never);
      const { component, navigate } = createLogin('ana', VALID_PASSWORD);

      await component.login();

      expect(component.message).toEqual({ headline: 'El backend respondió sin token.' });
      expect(storage.getItem('workshop_token')).toBeNull();
      expect(navigate).not.toHaveBeenCalled();
      expect(component.busy).toBe(false);
    });

    it('combina las preferencias guardadas con los valores por defecto', () => {
      storage.setItem('workshop_preferences', JSON.stringify({ theme: 'dark' }));

      const { component } = createLogin('ana', VALID_PASSWORD);

      expect(component.preferences).toEqual({ theme: 'dark', showTips: true, compact: false });
    });

    it('conserva las preferencias por defecto si el JSON guardado está corrupto', () => {
      storage.setItem('workshop_preferences', '{no-es-json');

      const { component } = createLogin('ana', VALID_PASSWORD);

      expect(component.preferences).toEqual({ theme: 'light', showTips: true, compact: false });
    });
  });

  describe('errores', () => {
    it('muestra el error de conexión cuando el servicio de autenticación no está disponible', async () => {
      mockedPost.mockRejectedValueOnce(new Error('Network Error'));
      const { component, navigate } = createLogin('ana', VALID_PASSWORD);

      await component.login();

      expect(component.message).toEqual({ headline: 'No se pudo autenticar a ana.', detail: 'Network Error' });
      expect(storage.getItem('workshop_token')).toBeNull();
      expect(navigate).not.toHaveBeenCalled();
      expect(component.busy).toBe(false);
    });

    it('muestra el mensaje del backend cuando responde 503 con cuerpo', async () => {
      mockedPost.mockRejectedValueOnce({
        message: 'Request failed with status code 503',
        response: { status: 503, data: { message: 'Servicio no disponible' } }
      });
      const { component } = createLogin('ana', VALID_PASSWORD);

      await component.login();

      expect(component.message).toEqual({ headline: 'No se pudo autenticar a ana.', detail: 'Servicio no disponible' });
    });

    it('muestra el error de timeout de 5000 ms como fallo de autenticación', async () => {
      mockedPost.mockRejectedValueOnce(
        Object.assign(new Error('timeout of 5000ms exceeded'), { code: 'ECONNABORTED' })
      );
      const { component, navigate } = createLogin('ana', VALID_PASSWORD);

      await component.login();

      expect(component.message).toEqual({ headline: 'No se pudo autenticar a ana.', detail: 'timeout of 5000ms exceeded' });
      expect(navigate).not.toHaveBeenCalled();
    });
  });

  describe('seguridad de mensajes', () => {
    const MALICIOUS_USERNAME = '<img src=x onerror=alert(1)>';

    it('trata el nombre de usuario como texto literal en el encabezado', async () => {
      mockedPost.mockRejectedValueOnce(new Error('Network Error'));
      const { component } = createLogin(MALICIOUS_USERNAME, VALID_PASSWORD);

      await component.login();

      expect(component.message).toEqual({
        headline: `No se pudo autenticar a ${MALICIOUS_USERNAME}.`,
        detail: 'Network Error'
      });
    });

    it('trata el mensaje de error del backend como texto literal', async () => {
      mockedPost.mockRejectedValueOnce({
        message: 'Request failed with status code 400',
        response: { status: 400, data: { message: '<b>Credenciales</b>' } }
      });
      const { component } = createLogin('ana', VALID_PASSWORD);

      await component.login();

      expect(component.message?.detail).toBe('<b>Credenciales</b>');
    });

    it('la plantilla no usa innerHTML para mostrar mensajes', () => {
      const template = readFileSync(new URL('./login.component.html', import.meta.url), 'utf8');

      expect(template).not.toContain('innerHTML');
    });
  });
});
