# Propuesta

## Por qué

El Issue #23 (H-AUTH-01, severidad alta) describe que entradas manipuladas en el login pueden alterar la consulta de credenciales y permitir acceso sin autenticación válida. Corregir este comportamiento evita que un intento de inyección conceda acceso y conserva el flujo de login esperado.

## Qué cambia

- El endpoint `POST /auth/login` debe aceptar las credenciales válidas existentes y rechazar entradas de inyección como credenciales inválidas.
- Los intentos de inyección en el usuario o la contraseña deben responder `401` y no incluir un token.
- Se preserva el contrato del login exitoso indicado por el Issue: respuesta `200` con `user.username` y `token`.

**Fuera de alcance:** cambios en endpoints distintos, en el formato de tokens, en el almacenamiento o ciclo de vida de contraseñas, y refactorizaciones generales de autenticación o de otros componentes. Esta propuesta define resultados observables, no una solución técnica.

## Capacidades

### Nuevas capacidades

- `auth-login`: autenticación mediante el contrato existente de `POST /auth/login`, incluyendo el rechazo de credenciales manipuladas.

### Capacidades modificadas

- Ninguna. El repositorio no contiene specs existentes.

## Impacto

- Issue de referencia: [#23 H-AUTH-01](https://github.com/user-bcp-student36/ai-labs/issues/23).
- Componente afectado: `auth/`, en el comportamiento de `POST /auth/login`.
- El Issue no solicita un cambio compatible-incompatible del contrato: mantiene el login válido y hace que las entradas de inyección se traten como credenciales inválidas.
- `docs/ENDPOINTS.md` no está presente en este checkout. La preservación especificada aquí se limita a lo comprometido explícitamente en el Issue y al comportamiento observable en `AuthResourceTest`; antes de implementar, debe contrastarse con el contrato Day 1 si ese documento está disponible.