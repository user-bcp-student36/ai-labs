# Design

## Contexto técnico

- **Origen:** [Issue #22 (HU-01)](https://github.com/user-bcp-student36/ai-labs/issues/22), H-AUTH-05.
- **Alcance aprobado:** web y api. El emisor del token (auth) queda fuera de este cambio.
- **Token actual:** Base64 de `usuario|rol|exp|secreto`. El secreto viaja dentro del token.
- **api hoy:** [api/app/main.py](api/app/main.py#L13) define `LEGACY_SECRET` como constante. [legacy_identity](api/app/main.py#L16-L26) decodifica Base64, separa por `|` y compara el cuarto campo con el secreto. Ese dependency se usa en todas las rutas protegidas (`/api/tasks`, `/api/summary`, `/api/requirements`, `/api/backend-health`). En el fallo devuelve `detail` con el texto de la excepción.
- **web hoy:** [DashboardComponent.ngOnInit](web/src/app/dashboard/dashboard.component.ts#L33) lee `workshop_token` de `localStorage`. Ante error HTTP muestra `error.response.data.detail` o `error.message` y no redirige. `logout()` borra `workshop_token` y `workshop_user`.
- **Contrato de error de api:** web lee `detail`. Ese campo debe mantenerse.

## Goals

1. api valida firma, expiración y claims del token antes de responder en `GET /api/tasks`, con HTTP 401 ante fallo.
2. api obtiene el secreto de `AUTH_TOKEN_SECRET` y no arranca sin él.
3. web trata el token como opaco y, ante 401 de `GET /api/tasks`, borra la sesión y redirige a `/login` con un mensaje.
4. Ningún componente registra el token ni el secreto.

## Non-goals

- Emisión o firma del token en auth, decisión HMAC frente a JWT en auth y gestión de su secreto (Issue #22 ajustes 1 a 6, lado auth).
- Tabla `legacy_users` (H-AUTH-03b).
- Corregir la consulta SQL con concatenación y el parámetro `owner` que decide el cliente. Son hallazgos de api distintos de H-AUTH-05; quedan registrados como riesgo, sin cambiarlos aquí.
- Rediseñar la autenticación de web o mover el token a cookies.

## Contratos protegidos

| Contrato | Qué no debe cambiar |
|---|---|
| `POST /auth/login` | Cuerpo `username`, `password`, `client`; respuesta `token`, `expiresIn`, `user`. |
| `GET /api/tasks` | Ruta, parámetro `owner`, cabecera `Authorization: Bearer`, lista de objetos con `title`, `description`, `status`, `priority`, `owner`, y códigos 200 y 401. |
| Error de api | Clave `detail` en respuestas de error (web la lee). |
| Claves de `localStorage` | `workshop_token` y `workshop_user` mantienen sus nombres. |

## Componentes afectados

- **web:** `DashboardComponent` (manejo de 401), `LoginComponent` (guardado del token, sin cambio de contrato) y un módulo nuevo de sesión, por ejemplo `web/src/app/utils/session.ts`, que centraliza las claves y las operaciones de guardar y borrar.
- **api:** `legacy_identity` en [api/app/main.py](api/app/main.py#L16-L26) y la constante `LEGACY_SECRET`. Afecta a todas las rutas protegidas, no solo a `/api/tasks`.
- **Pruebas:** [api/tests/test_api.py](api/tests/test_api.py) (su helper de token usa el secreto legacy y dejará de servir), `web/src/app/dashboard/dashboard.component.spec.ts` y `web/src/app/login/login.component.spec.ts`.
- **Configuración:** `compose.yaml` debe definir `AUTH_TOKEN_SECRET` para api y auth antes de desplegar.
- **Documentación:** [docs/ENDPOINTS.md](docs/ENDPOINTS.md), sección de comportamiento ante 401.

## Decisiones técnicas y alternativas

| Decisión | Opción elegida | Alternativa | Motivo |
|---|---|---|---|
| Algoritmo de firma en api | Pendiente de la decisión documentada (criterio 11 del Issue). Propuesta: HS256 con una librería JWT que fije `algorithms=["HS256"]`. | HMAC propio con biblioteca estándar (`hmac`, `hashlib`). | Una librería evita errores de implementación y del tipo "alg none". Añade una dependencia que hay que justificar. El HMAC propio no tiene dependencias, pero requiere más código crítico propio. |
| Alcance de la validación en api | Cambiar el dependency compartido, de modo que todas las rutas protegidas exijan firma. | Validar solo `/api/tasks`. | Si otras rutas siguen aceptando el formato Base64, H-AUTH-05 queda abierto. La spec solo describe `/api/tasks`; hay que ampliarla o confirmar el alcance. |
| Transición de tokens legacy | Corte coordinado con auth: api deja de aceptar Base64 al desplegar. | Flag `API_ACCEPT_LEGACY_TOKENS` (por defecto apagado) para una ventana de coordinación. | El flag permite desplegar sin romper sesiones, pero mantiene la aceptación de tokens sin firma mientras esté activo. Sugerido solo si auth y api no pueden desplegarse juntos. |
| Respuesta ante token inválido | 401 con `detail` genérico, por ejemplo `"Token inválido"`. | Mantener `detail` con el texto de la excepción (estado actual). | El texto de la excepción puede exponer detalles internos. Web solo necesita el código 401. |
| Almacenamiento en web | `localStorage` sin cambios. | Cookies `HttpOnly`. | Fuera de alcance; se registra como deuda ya señalada en `docs/ENDPOINTS.md`. |
| Límite de expiración en web | Ninguno; web reacciona al 401. | Comprobar `expiresIn` localmente. | La spec no lo pide y evita depender del reloj del cliente. |

## Estrategia incremental

Cada paso debe quedar verde antes del siguiente. Los pasos 1 y 2 son independientes del backend y pueden desplegarse por separado.

1. **Caracterización (sin cambio de comportamiento).**
   - api: login legacy aceptado en `/api/tasks`, token expirado rechazado, secreto incorrecto rechazado, respuesta de error con `detail`.
   - web: sin token redirige a `/login`; respuesta 200 renderiza tareas; error 500 muestra mensaje y conserva el token; error de red muestra mensaje.
2. **Web: módulo de sesión.** Centralizar las claves en `session.ts` y sustituir las literales de login y dashboard. Sin cambio funcional.
3. **Web: manejo de 401.** Ante 401 en `GET /api/tasks`, borrar `workshop_token`, `workshop_user` y `workshop_preferences`, y redirigir a `/login` con mensaje. Actualizar las pruebas de caracterización del paso 1 que cambian a propósito.
4. **api: validador de firma.** Añadir la validación de firma, expiración y claims en el dependency, con los casos de la spec. Las pruebas usan un token firmado con un secreto de prueba inyectado por entorno.
5. **api: retirar el secreto legacy.** Eliminar `LEGACY_SECRET`, exigir `AUTH_TOKEN_SECRET` al arrancar y ajustar el helper de `test_api.py`.
6. **Coordinación con auth.** Desplegar auth con el token firmado y api en el mismo corte, o con el flag del paso 4 si se usa esa alternativa.
7. **Documentación.** Actualizar `docs/ENDPOINTS.md` con el 401 y el cierre de sesión en web.

Cobertura objetivo: 85% o más del código modificado en web (vitest) y api (pytest).

## Pruebas de caracterización necesarias antes de modificar

Son obligatorias antes del paso 3 y el paso 5, porque cambian comportamiento crítico:

- api: `GET /api/tasks` con token Base64 legacy válido (estado actual: 200), con token expirado (401), con secreto distinto (401) y sin cabecera (401). Mantener también `test_lists_seeded_tasks` y `test_rejects_invalid_token`.
- web: el dashboard con 401 hoy no redirige; la prueba debe reflejar el estado actual antes del cambio y luego actualizarse al nuevo comportamiento.
- web: el token no aparece en `console` ni en el texto renderizado en los casos de error.

## Riesgos y trade-offs

- **Corte de sesiones:** tras el despliegue, los tokens legacy de `localStorage` dejan de valer. El 401 con redirección lo absorbe, pero el usuario debe volver a iniciar sesión.
- **Despliegue desacoplado:** si api valida firma antes de que auth emita tokens firmados, todo login queda inútil. Es el riesgo principal; mitigarlo con el corte coordinado del paso 6.
- **Secreto ausente:** si `AUTH_TOKEN_SECRET` no está en `compose.yaml`, api no arranca. Verificarlo antes de desplegar.
- **Ampliar el alcance de api:** cambiar el dependency compartido afecta a todas las rutas. Es más seguro, pero amplía las pruebas necesarias.
- **Mensaje de error de api:** cambiar `detail` puede romper el mensaje mostrado por web si web depende del texto exacto. Mantener la clave.
- **Riesgos no resueltos:** SQL concatenado en api y el parámetro `owner` controlado por el cliente siguen abiertos; no los corrige este cambio.

## Rollback

- **web:** revertir los commits de los pasos 2 y 3. El cambio no altera el contrato, así que la versión anterior sigue funcionando con la api actual.
- **api:** revertir el commit de los pasos 4 y 5 restaura el formato Base64. Si auth ya emite tokens firmados, hay que revertir también auth o usar el flag, porque el token firmado no se valida en la versión anterior.
- **Coordinado:** si el corte del paso 6 falla, revertir auth y api juntos; web es compatible en ambos sentidos.
