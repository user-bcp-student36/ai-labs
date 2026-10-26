# Hallazgos

Estados: Pendiente, Corregido, Parcial, No encontrado. Evidencia desde la raíz.

## auth

| ID | Área | Caso | Estado | Evidencia |
|---|---|---|---|---|
| H-AUTH-01 | SCA | Commons Text 1.9 (CVE-2022-42889) | Corregido | Dependencia removida de [auth/pom.xml](auth/pom.xml); ningún código la usa |
| H-AUTH-02 | SCA | Quarkus 3.27.4 con advisories | Pendiente | No verificado; existe rama dependabot `quarkus.platform.version-3.40.1.1` sin evaluar |
| H-AUTH-03 | Datos | Contraseñas en texto plano | Corregido | PBKDF2WithHmacSHA256, 600k iteraciones, salt 16 bytes en [PasswordHasher.java](auth/src/main/java/com/bcp/workshop/auth/security/PasswordHasher.java); pruebas en [PasswordHasherTest.java](auth/src/test/java/com/bcp/workshop/auth/security/PasswordHasherTest.java) |
| H-AUTH-03b | Datos | Tabla `legacy_users` con texto plano en BDs existentes | Pendiente | Purga manual; decisión humana |
| H-AUTH-04 | SQL | Concatenación de credenciales | Corregido | `PreparedStatement` en [JdbcUserRepository.java](auth/src/main/java/com/bcp/workshop/auth/repository/JdbcUserRepository.java); prueba de apóstrofe en [AuthLoginContractTest.java](auth/src/test/java/com/bcp/workshop/auth/AuthLoginContractTest.java) |
| H-AUTH-05 | Token | Base64 con secreto embebido, sin firma real | Pendiente | Formato conservado por compatibilidad con `api/app/main.py`; requiere cambio coordinado con api |
| H-AUTH-06 | Configuración | CORS, URL de BD y contraseñas semilla hardcodeados | Parcial | Externalizados en [application.properties](auth/src/main/resources/application.properties) con variables de entorno; el secreto del token sigue sin externalizar |
| H-AUTH-07 | Clean code | Resource con validación, negocio, persistencia y respuesta | Corregido | Capas en `service/`, `repository/`, `security/`; [AuthResource.java](auth/src/main/java/com/bcp/workshop/auth/AuthResource.java) solo mapea HTTP |
| H-AUTH-08 | Errores | Respuesta 500 expone clase y mensaje interno | Corregido | Mensaje genérico en [AuthResource.java](auth/src/main/java/com/bcp/workshop/auth/AuthResource.java); la rama 500 no tiene prueba HTTP (cobertura 75% de ese archivo) |
