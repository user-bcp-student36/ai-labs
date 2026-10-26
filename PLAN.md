# Plan

Estado general: aprobaciones registradas según la respuesta del usuario en la sesión. H-AUTH-06 fase A implementada. H-AUTH-02 investigado sin cambio de versión. H-AUTH-03b opción B aplicada como documentación. H-AUTH-05 aprobado con condición de coordinación con api, no realizada, no implementado.

## auth

### Selección

Fuente: [HALLAZGOS.md](HALLAZGOS.md). Hallazgos `Pendiente` de auth seleccionados:

| ID | Motivo de selección | Bloqueo |
|---|---|---|
| H-AUTH-06 | Riesgo alto de secreto hardcodeado; alcance acotado a auth | Ninguno para la parte de auth |
| H-AUTH-05 | Riesgo alto; token legado con secreto embebido | Requiere cambio coordinado con api |
| H-AUTH-03b | Riesgo alto de datos en texto plano en BDs existentes | Requiere decisión humana de purga |
| H-AUTH-02 | Riesgo de SCA sobre Quarkus 3.27.4 | Advisories no verificados |

H-AUTH-01, H-AUTH-03, H-AUTH-04, H-AUTH-07 y H-AUTH-08 están `Corregido` y no entran en este plan.

### Contrato observado de POST /auth/login

No existe `docs/ENDPOINTS.md`, así que este contrato se toma del código actual. Es el comportamiento que se conserva.

- Entrada: JSON con `username`, `password` y `client`; campos desconocidos se ignoran.
- 200: `{ token, expiresIn: 3600, user: { username, displayName, role }, debug: "authenticated-with-legacy-flow" }`. Ver [AuthResource.java](auth/src/main/java/com/bcp/workshop/auth/AuthResource.java#L23) y [AuthService.java](auth/src/main/java/com/bcp/workshop/auth/service/AuthService.java#L29).
- 400: `Username is required`, `Password is required` o `Request is null`. Ver [AuthService.java](auth/src/main/java/com/bcp/workshop/auth/service/AuthService.java#L29-L30).
- 401: `Invalid username or password`. Ver [AuthService.java](auth/src/main/java/com/bcp/workshop/auth/service/AuthService.java#L16).
- 500: `Unexpected error` genérico. Ver [AuthResource.java](auth/src/main/java/com/bcp/workshop/auth/AuthResource.java#L24).

Desviación ya presente frente al código original: el 401 original era `Invalid user <usuario> or password`, y el 500 original incluía el mensaje y la clase de excepción. Esa desviación no está aprobada por una persona; se registra como decisión pendiente.

Riesgos pendientes del contrato: el campo `token` tiene formato legado que `api/app/main.py` consume; cualquier cambio de formato rompe a api.

### H-AUTH-06 — Configuración: secreto del token hardcodeado

Problema: el secreto `WORKSHOP_SECRET_2026` está en código, en [TokenIssuer.java](auth/src/main/java/com/bcp/workshop/auth/security/TokenIssuer.java#L13), y se concatena al token en [TokenIssuer.java](auth/src/main/java/com/bcp/workshop/auth/security/TokenIssuer.java#L18). El hallazgo queda `Parcial`: CORS, URL de BD y semillas ya están externalizadas.

Cambio propuesto (fase A, sin cambiar el formato):
- Añadir `auth.token.secret=${AUTH_TOKEN_SECRET:WORKSHOP_SECRET_2026}` en [application.properties](auth/src/main/resources/application.properties). El valor por defecto conserva la compatibilidad con api mientras no se cambie la variable.
- Inyectar el valor en `TokenIssuer` con `@ConfigProperty`, en lugar de la constante.
- Quitar la constante `SHARED_SECRET` de la clase.
- Declarar la variable `AUTH_TOKEN_SECRET` en la documentación de configuración. No se agrega ningún secreto real al repositorio.

Archivos: `auth/src/main/java/com/bcp/workshop/auth/security/TokenIssuer.java`, `auth/src/main/resources/application.properties`, `auth/src/test/java/com/bcp/workshop/auth/security/TokenIssuerTest.java`.

Prueba de caracterización previa:
- Extender [AuthLoginContractTest.java](auth/src/test/java/com/bcp/workshop/auth/AuthLoginContractTest.java) para verificar que el cuarto segmento del token decodificado sigue siendo `WORKSHOP_SECRET_2026` con la configuración por defecto.
- Prueba unitaria de `TokenIssuer` con el secreto inyectado, que verifique que el segmento cambia al cambiar el valor configurado.

Clean code: `issue` separa en tres métodos de una sola tarea: construir claims, codificar, y devolver el token. La expiración sigue en la constante `TTL_SECONDS`.

Decisión humana: Aprobada (fase A), según respuesta del usuario. Implementada: `auth.token.secret` con `AUTH_TOKEN_SECRET`, mismo valor por defecto. Estado en HALLAZGOS.md: Resuelto, con el literal por defecto pendiente de retirar con H-AUTH-05.

### H-AUTH-05 — Token: Base64 con secreto embebido, sin firma

Problema: el token es `Base64(username|role|exp|secreto)` en [TokenIssuer.java](auth/src/main/java/com/bcp/workshop/auth/security/TokenIssuer.java#L16-L19). Cualquiera que lo lea obtiene el secreto y puede forjar tokens. Las [buenas prácticas de auth](docs/GOOD_PRACTICES.md) piden tokens firmados, con expiración y claims explícitos.

Cambio propuesto (fase B, requiere coordinación):
- Emitir un JWT firmado con HMAC-SHA256 usando el secreto de H-AUTH-06, con claims `sub`, `role`, `iat`, `exp`, `iss` y `aud`.
- Construir el token con una clase `TokenSigner` (firma HMAC) y una clase `TokenClaims` (armado de claims), separadas de `TokenIssuer`.
- Usar `expiresIn` igual a `TTL_SECONDS`, sin cambiar la respuesta JSON de login.

Esto cambia el formato del token. `api/app/main.py` valida el formato legado; por eso no puede cambiarse sin un cambio coordinado y revisado en api. Este plan no modifica api.

Archivos auth: `security/TokenIssuer.java`, nuevas clases en `security/`, `security/TokenIssuerTest.java`, `AuthLoginContractTest.java`.

Prueba de caracterización previa: la prueba actual del token legado (4 segmentos, expiración dentro de TTL) se conserva como caracterización hasta que la fase B se apruebe. Después de la fase B se reemplaza por pruebas de firma, emisor, audiencia y expiración.

Dependencias externas: coordinación con el equipo de api para verificación de firma. Sin esa coordinación, la fase B no se implementa.

Decisión humana: Aprobada para investigar y registrar. Aprobación condicionada a coordinación con api; coordinación no realizada, cambio no implementado. Estado en HALLAZGOS.md: Pendiente.

### H-AUTH-03b — Datos: tabla `legacy_users` con texto plano

Problema: BDs creadas por la versión original contienen una tabla `legacy_users` con contraseñas en texto plano. El código actual ya no la usa (ver [JdbcUserRepository.java](auth/src/main/java/com/bcp/workshop/auth/repository/JdbcUserRepository.java#L19)), pero los datos siguen en disco.

Cambio propuesto:
- Opción A (recomendada): script de limpieza documentado y ejecutado manualmente: `DROP TABLE IF EXISTS legacy_users;` sobre cada BD existente, después de respaldo.
- Opción B: no tocar datos; solo documentar que la tabla queda huérfana.

No se propone un borrado automático en el arranque, porque destruye datos sin confirmación.

Archivos: ninguno de código. Documentación en [docs/TESTING.md](docs/TESTING.md) o en un documento de operación, según decida el equipo.

Prueba de caracterización previa: en una BD temporal con `legacy_users` creada manualmente, verificar que `UserStoreInitializer` arranca y que `findByUsername` no consulta esa tabla.

Decisión humana: Opción B aprobada, solo documentar; sin purga ni cambio de código. Estado en HALLAZGOS.md: Pendiente, riesgo residual aceptado por decisión humana.

### H-AUTH-02 — SCA: Quarkus 3.27.4

Problema: el hallazgo original pide investigar advisories de Quarkus `3.27.4`. Esta verificación no se ha hecho. Existe una rama remota de dependabot para `quarkus.platform.version-3.40.1.1`, pero no se ha evaluado.

Cambio propuesto:
- Investigar advisories de Quarkus 3.27.4 en la fuente oficial (GitHub Advisory Database o notas de Quarkus) y registrar identificadores y estado en HALLAZGOS.md.
- Si hay CVE aplicable, actualizar `quarkus.platform.version` en [auth/pom.xml](auth/pom.xml) a la versión corregida mínima y repetir `python tools/lab.py test auth`.
- Si no hay CVE aplicable, cerrar el hallazgo con evidencia.

Archivos: `auth/pom.xml`, HALLAZGOS.md.

Prueba de caracterización previa: ejecutar `python tools/lab.py test auth` antes de cambiar la versión, para registrar la línea base de las 25 pruebas.

Dependencias: actualizar Quarkus puede cambiar comportamiento de REST o CDI; la línea base debe quedar verde antes.

Decisión humana: Investigación aprobada. Actualización de versión pendiente de confirmar la versión objetivo (sugerida 3.27.5). Estado en HALLAZGOS.md: Pendiente.

### Registro de ejecución (Día 1, auth)

- Línea base: 25/25 pruebas, BUILD SUCCESS.
- H-AUTH-06 fase A: prueba de caracterización 25/25 antes del cambio; cambio aplicado; corrección de `AuthServiceTest` (`new TokenIssuer()` sin argumento); 26/26 pruebas, BUILD SUCCESS. Cobertura de `TokenIssuer`: 100% de líneas (6/6) en JaCoCo.
- H-AUTH-02: investigación de advisories; sin cambio en `auth/pom.xml`.
- H-AUTH-03b: opción B, sin cambio de código.
- H-AUTH-05: no implementado; bloqueado por coordinación con api.
- CodeQL y Dependency Review remotos: no ejecutados.

### Pendientes transversales

- Crear `docs/ENDPOINTS.md`: no existe; no se crea en este paso porque el plan no lo pide. Debe registrarse el contrato observado y acordado.
- Coordinar con api el cambio de formato de token (H-AUTH-05). Sin coordinación no hay implementación de fase B.
- Registrar la decisión humana sobre la desviación del 401 y del 500 frente al original.
- `docs/GOOD_PRACTICES.md` menciona `UserRecord`, que fue eliminada; la convención de DTO se cumple con `UserAccount` y `LoginResponse`. No se edita esa guía en este paso.
- La guía de auth menciona BCrypt como ejemplo; la implementación usa PBKDF2 con 600000 iteraciones. Es compatible con la convención, que dice "por ejemplo".
