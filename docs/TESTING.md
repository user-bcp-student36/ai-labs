# Pruebas y cobertura

## auth

Comando: `python tools/lab.py test auth` (ejecuta `mvn verify` en `auth/`).

Resultado: 25 pruebas, 0 fallos, 0 errores. BUILD SUCCESS.

| Clase de prueba | Tipo | Casos |
|---|---|---|
| AuthLoginContractTest | Caracterización (@QuarkusTest) | 8: login válido, token legado, contraseña incorrecta, usuario inexistente, apóstrofe, campos vacíos, campos desconocidos |
| AuthResourceTest | Original | 1 |
| HtmlEscaperTest | Unitaria | 2 |
| JdbcUserRepositoryTest | Unitaria con SQLite temporal | 3 |
| UserStoreInitializerTest | Unitaria | 1 |
| PasswordHasherTest | Unitaria | 4 |
| TokenIssuerTest | Unitaria | 1 |
| AuthServiceTest | Unitaria con repositorio en memoria | 5 |

Cobertura medida con JaCoCo 0.8.12 (`auth/target/site/jacoco/jacoco.csv`, `auth/target/site/jacoco/index.html`).

Método: líneas cubiertas / (cubiertas + perdidas), leídas del CSV de JaCoCo.

- Global del módulo: 90.7% (líneas cubiertas y perdidas sumadas sobre las 14 clases del CSV).
- Código modificado (las mismas 14 clases, sin `LoginRequest` que no cambió): 90.6%. Cumple la meta de 85%.
- Clases por debajo de 85%: `AuthResource` 75% (la rama 500 no tiene prueba HTTP). `JdbcUserRepository` 86% queda por debajo de 90%; no se analizaron sus líneas sin cubrir.

Límites: la cobertura de Quarkus en arranque no se mide por archivo de forma fiable; `UserStoreInitializer` se cubre con prueba unitaria directa.

Pendientes de prueba: no se ejecutó contra una BD `legacy_users` existente; no se probó el flujo completo con `api`.
