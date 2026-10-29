# Tasks

Cambio: `hu-01-token-firmado-web`. Origen: [Issue #22 (HU-01)](https://github.com/user-bcp-student36/ai-labs/issues/22), H-AUTH-05.

Componentes: web (bloques 1 a 3) y api (bloques 4 a 6). auth queda fuera de alcance. Ejecutar desde la raíz de ai-labs con `python tools/lab.py test web` o `python tools/lab.py test api`. Sin push, remotos, tags ni PR durante el Día 1.

## 0. Prerrequisitos y decisiones previas

- [ ] 0.1 Confirmar la aprobación humana de proposal.md, specs/ y design.md.
- [ ] 0.2 Confirmar con el equipo de auth el formato del token firmado: claims `sub`, `client`, `iat`, `exp`, algoritmo y secreto compartido `AUTH_TOKEN_SECRET`. Bloquea el bloque 5.
- [ ] 0.3 Decidir el algoritmo de firma (criterio 11 del Issue): librería JWT con HS256 o HMAC propio. Bloquea 5.1.
- [ ] 0.4 Verificar en `compose.yaml` que `AUTH_TOKEN_SECRET` se pasa a api desde el entorno, sin valor literal. Bloquea 6.4.

## 1. Web: pruebas de caracterización (sin cambios de código)

- [x] 1.1 Leer `web/src/app/dashboard/dashboard.component.spec.ts` y `web/src/app/login/login.component.spec.ts` y anotar los casos existentes.
- [x] 1.2 Prueba: el login con HTTP 200 guarda el valor exacto de `token` en `workshop_token`.
- [x] 1.3 Prueba: el dashboard sin `workshop_token` redirige a `/login` y no llama a `GET /api/tasks`.
- [x] 1.4 Prueba: el dashboard envía `Authorization: Bearer <valor exacto>` en `GET /api/tasks`.
- [x] 1.5 Prueba: ante HTTP 500 muestra un error y conserva `workshop_token` (comportamiento actual).
- [x] 1.6 Prueba: ante error de red no hay token en texto ni consola.
- [x] 1.7 Ejecutar `python tools/lab.py test web`; todas las pruebas deben pasar antes del bloque 2.

## 2. Web: módulo de sesión (refactor sin cambio funcional)

- [x] 2.1 Crear `web/src/app/utils/session.ts` con las claves `workshop_token`, `workshop_user` y `workshop_preferences`, y funciones para guardar, leer y borrar la sesión.
- [x] 2.2 Crear pruebas unitarias para `session.ts` en `web/src/app/utils/session.spec.ts`.
- [x] 2.3 Sustituir las claves literales de `login.component.ts` y `dashboard.component.ts` por `session.ts`.
- [x] 2.4 Ejecutar `python tools/lab.py test web`; las pruebas de caracterización del bloque 1 siguen pasando.

## 3. Web: manejo de HTTP 401

- [x] 3.1 Escribir prueba (roja): HTTP 401 en `GET /api/tasks` borra la sesión, redirige a `/login` y muestra un mensaje de sesión expirada.
- [x] 3.2 Escribir prueba (roja): HTTP 401 no se confunde con HTTP 500; el 500 conserva la sesión (cubierto por 1.5).
- [x] 3.3 Implementar en `dashboard.component.ts` la rama de 401 en el manejo de error: limpiar sesión y navegar a `/login`.
- [x] 3.4 Ajustar `login.component.ts` para mostrar el mensaje de sesión expirada cuando llegue por redirección.
- [x] 3.5 Prueba: ni la consola ni la interfaz contienen el valor del token en los casos de error.
- [x] 3.6 Ejecutar `python tools/lab.py test web`; cobertura del código modificado ≥85%.

## 4. API: pruebas de caracterización (antes de cambiar la validación)

- [ ] 4.1 Prueba: `GET /api/tasks` con el token Base64 actual válido responde 200 (se reemplaza en 5.x).
- [ ] 4.2 Prueba: `GET /api/tasks` sin encabezado `Authorization` responde 401.
- [ ] 4.3 Prueba: token con secreto distinto responde 401.
- [ ] 4.4 Prueba: la respuesta de error contiene la clave `detail`.
- [ ] 4.5 Ejecutar `python tools/lab.py test api`; todas las pruebas deben pasar antes del bloque 5.

## 5. API: validación de token firmado

- [ ] 5.1 Añadir la dependencia elegida en 0.3 a `api/requirements.txt` con su justificación en el PR.
- [ ] 5.2 Prueba (roja): token firmado válido responde 200.
- [ ] 5.3 Prueba (roja): firma inválida y payload alterado responden 401.
- [ ] 5.4 Prueba (roja): token expirado, malformado o sin claims `sub`, `client`, `iat`, `exp` responden 401.
- [ ] 5.5 Crear `api/app/token_validator.py` con la verificación de firma, expiración y claims. Sin lógica de rutas.
- [ ] 5.6 Reemplazar `legacy_identity` en `api/app/main.py` por el validador, manteniendo la dependencia compartida de las rutas protegidas.
- [ ] 5.7 Responder 401 con `detail` genérico (`"Token inválido"`), sin el texto de la excepción.
- [ ] 5.8 Prueba con `caplog`: los logs no contienen el token ni el secreto cuando la validación falla.

## 6. API: configuración del secreto

- [ ] 6.1 Prueba: el arranque sin `AUTH_TOKEN_SECRET` falla con un mensaje que nombra la variable y no muestra valores.
- [ ] 6.2 Leer `AUTH_TOKEN_SECRET` en la configuración y eliminar `LEGACY_SECRET` de `api/app/main.py`.
- [ ] 6.3 Actualizar el helper `token()` de `api/tests/test_api.py` para firmar con un secreto de prueba inyectado por entorno.
- [ ] 6.4 Pasar `AUTH_TOKEN_SECRET` a api en `compose.yaml` desde el entorno, sin valor literal.
- [ ] 6.5 Ejecutar `python tools/lab.py test api`; cobertura del código modificado ≥85%.

## 7. Integración y documentación

- [ ] 7.1 Verificar login → web → api con un token firmado por auth. Depende del gate 0.2; si auth no está listo, registrar el bloqueo y no marcar esta tarea.
- [ ] 7.2 Actualizar `docs/ENDPOINTS.md` con el comportamiento ante 401 en web y api.
- [ ] 7.3 Revisar que `POST /auth/login` y `GET /api/tasks` mantienen forma y códigos en `docs/ENDPOINTS.md`.
- [ ] 7.4 Registrar la evidencia en la raíz con IDs H-WEB-NN y H-API-NN, conservando las secciones existentes.
- [ ] 7.5 Ejecutar `openspec validate hu-01-token-firmado-web --strict` desde la raíz.
