# Hallazgos

Estados: Pendiente, Corregido, Parcial, No encontrado. Evidencia desde la raíz.

## auth

| ID | Área | Caso | Estado | Evidencia |
|---|---|---|---|---|
| H-AUTH-01 | SCA | Commons Text 1.9 (CVE-2022-42889) | Corregido | Dependencia removida de [auth/pom.xml](auth/pom.xml); ningún código la usa |
| H-AUTH-02 | SCA | Quarkus 3.27.4 con advisories | Pendiente | Advisories revisadas (GHSA-qcxp-gm7m-4j5v / CVE-2026-50559, GHSA-rc95-pcm8-65v9 / CVE-2026-39852). Rango afectado de 3.27.x ambiguo para 3.27.4; parche listado 3.27.5. Explotación requiere políticas de ruta `quarkus.http.auth` que auth no define. Sin cambio de versión: falta confirmar versión objetivo (sugerida 3.27.5) |
| H-AUTH-03 | Datos | Contraseñas en texto plano | Corregido | PBKDF2WithHmacSHA256, 600k iteraciones, salt 16 bytes en [PasswordHasher.java](auth/src/main/java/com/bcp/workshop/auth/security/PasswordHasher.java); pruebas en [PasswordHasherTest.java](auth/src/test/java/com/bcp/workshop/auth/security/PasswordHasherTest.java) |
| H-AUTH-03b | Datos | Tabla `legacy_users` con texto plano en BDs existentes | Pendiente | Opción B aprobada: solo documentar, sin purga. Código actual no consulta `legacy_users`; los datos siguen en disco, riesgo residual aceptado por decisión humana |
| H-AUTH-04 | SQL | Concatenación de credenciales | Corregido | `PreparedStatement` en [JdbcUserRepository.java](auth/src/main/java/com/bcp/workshop/auth/repository/JdbcUserRepository.java); prueba de apóstrofe en [AuthLoginContractTest.java](auth/src/test/java/com/bcp/workshop/auth/AuthLoginContractTest.java) |
| H-AUTH-05 | Token | Base64 con secreto embebido, sin firma real | Pendiente | Aprobado condicionado a coordinación con api, no realizada; no implementado. Formato conservado por compatibilidad con `api/app/main.py` |
| H-AUTH-06 | Configuración | CORS, URL de BD, contraseñas semilla y secreto del token hardcodeados | Resuelto | Secreto leído desde `auth.token.secret` (`AUTH_TOKEN_SECRET`) en [TokenIssuer.java](auth/src/main/java/com/bcp/workshop/auth/security/TokenIssuer.java#L20); valor por defecto idéntico en [application.properties](auth/src/main/resources/application.properties) para no romper api. El literal por defecto permanece y se retira con H-AUTH-05 |
| H-AUTH-07 | Clean code | Resource con validación, negocio, persistencia y respuesta | Corregido | Capas en `service/`, `repository/`, `security/`; [AuthResource.java](auth/src/main/java/com/bcp/workshop/auth/AuthResource.java) solo mapea HTTP |
| H-AUTH-08 | Errores | Respuesta 500 expone clase y mensaje interno | Corregido | Mensaje genérico en [AuthResource.java](auth/src/main/java/com/bcp/workshop/auth/AuthResource.java); la rama 500 no tiene prueba HTTP (cobertura 75% de ese archivo) |
