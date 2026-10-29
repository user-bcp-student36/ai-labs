# Proposal

Origen: [Issue #22 (HU-01)](https://github.com/user-bcp-student36/ai-labs/issues/22), derivado de H-AUTH-05 en HALLAZGOS.md.

## Why

El Issue #22 describe un token Base64 sin firma que expone el secreto compartido y puede ser alterado. La parte que corrige ese problema vive en auth (TokenIssuer) y en la validación de api. El componente web consume ese token en `POST /auth/login` y lo reenvía como `Authorization: Bearer` en `GET /api/tasks`, por lo que el cliente web debe tratarlo como opaco, conservarlo y reaccionar correctamente cuando el backend lo rechace. Sin ese comportamiento, un token firmado inválido o expirado deja la aplicación web en un estado inconsistente.

Alcance: componentes web y api. Los criterios de aceptación del Issue #22 que exigen cambios en auth quedan fuera de este cambio y se tratan en una propuesta aparte.

## What Changes

- El cliente web trata el token devuelto por `POST /auth/login` como una cadena opaca: no lo decodifica, no lo interpreta y no extrae de él datos de sesión.
- El cliente web sigue enviando el token únicamente como `Authorization: Bearer` en `GET /api/tasks`, sin cambiar la forma de la petición.
- Cuando `GET /api/tasks` responde HTTP 401, el cliente web elimina la sesión almacenada y dirige al usuario a la pantalla de login con un mensaje comprensible.
- El cliente web no escribe en logs, consola ni interfaz el valor del token ni datos sensibles.
- El servicio api verifica la firma, la expiración y los claims del token antes de responder `GET /api/tasks`. Un token con firma inválida, alterado o expirado recibe HTTP 401.
- El servicio api obtiene el secreto de verificación desde la variable de entorno `AUTH_TOKEN_SECRET`; si no está definida, el servicio no arranca.
- El servicio api no registra tokens ni secretos en logs.
- Se mantienen el contrato de `POST /auth/login` y de `GET /api/tasks` documentado en docs/ENDPOINTS.md. Ningún cambio de contrato es **BREAKING** para el frontend.

Fuera de alcance:

- Emisión, firma del token y claims emitidos por auth (Issue #22 ajustes 1 a 4).
- Decisión entre HMAC propio y libreria JWT (Issue #22 criterio 11).
- Gestión de `AUTH_TOKEN_SECRET` y de logs en auth (Issue #22 ajustes 5 y 6).
- Tabla `legacy_users` (H-AUTH-03b).

## Capabilities

### New Capabilities

- `web-auth-session`: comportamiento de la sesión del cliente web frente al token de login: almacenamiento opaco, envío como Bearer en las peticiones protegidas y cierre de sesión ante respuestas 401.
- `api-token-validation`: verificación del token firmado en `GET /api/tasks`: firma, expiración, rechazo con HTTP 401 y secreto obtenido de configuración.

### Modified Capabilities

Ninguna. No existen specs en `openspec/specs/` todavía.

## Impact

- Código afectado (web): `web/src/app/login/login.component.ts` (almacenamiento del token), `web/src/app/dashboard/dashboard.component.ts` (envío de Bearer y manejo de 401) y el servicio o función de sesión que se defina en la fase de diseño.
- Contratos: `POST /auth/login` y `GET /api/tasks` sin cambios de forma; ver docs/ENDPOINTS.md.
- Código afectado (api): `api/app/main.py` (validación del token, hoy Base64 en las líneas 19 a 21) y `api/tests/test_api.py` (casos de firma válida e inválida).
- Dependencia: api solo puede validar tokens firmados si auth los emite con el formato acordado. Sin ese cambio en auth, api rechazaría los tokens actuales. El formato debe coordinarse entre los equipos de auth y api antes de implementar.
- Pruebas: pruebas unitarias en vitest para el manejo de 401 y del token opaco en web; pruebas en pytest para la validación del token en api. Cobertura de 85% o más en el código modificado de ambos componentes.
- Documentación: actualizar docs/ENDPOINTS.md con el comportamiento ante 401 del lado web.
