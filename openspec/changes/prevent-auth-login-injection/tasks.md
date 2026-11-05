# Tasks

## 1. Pruebas de caracterización

- [x] 1.1 Conservar en `AuthResourceTest` el caso de login válido (`student` / `Workshop123!`) y agregar casos para la cadena `' OR '1'='1` en usuario y contraseña; comprobar que el login válido pasa y que, antes de la corrección, los casos de inyección reproducen el bypass en vez de devolver `401` sin token.

## 2. Protección de la búsqueda de credenciales

- [x] 2.1 Actualizar solo la consulta de `LegacyDatabase.findByCredentials` para enlazar usuario y contraseña como parámetros y conservar el mapeo de resultados; comprobar que los tres escenarios de `AuthResourceTest` pasan y que ambos intentos de inyección devuelven `401` sin token.

## 3. Validación del componente

- [x] 3.1 Ejecutar `mvn test` desde `auth/` y `openspec validate prevent-auth-login-injection --strict` desde la raíz; confirmar que ambos comandos pasan sin cambios fuera del alcance de `auth/` y este change OpenSpec.