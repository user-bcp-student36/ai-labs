# Diseño

## Contexto

Ver [proposal.md](proposal.md) para la motivación y [specs/auth-login/spec.md](specs/auth-login/spec.md) para los requisitos observables.

En `auth/`, `LegacyDatabase.findByCredentials` construye actualmente la condición SQL concatenando el usuario y la contraseña, y ejecuta la consulta con `Statement`. `AuthResource.login` delega en ese método: cuando no se obtiene un usuario responde `401`; cuando sí se obtiene, genera el token y devuelve los datos del usuario. `AuthResourceTest` ya caracteriza el inicio de sesión exitoso para `student` / `Workshop123!`, incluyendo `user.username` y un token no nulo.

`docs/ENDPOINTS.md`, indicado como contrato Day 1, no existe en el checkout actual. Por tanto, este diseño protege los campos y estados explícitos en el Issue y en la prueba existente, y evita modificar el resto de la respuesta hasta que el contrato documentado esté disponible.

## Goals / Non-Goals

**Goals:**

- Hacer que ambas credenciales se comparen como valores, sin permitir que su contenido altere la condición de autenticación.
- Mantener el login válido existente y el mapeo de resultados que alimenta la respuesta actual.
- Demostrar por pruebas de endpoint tanto el rechazo de inyección en usuario como en contraseña.

**Non-Goals:**

- Cambiar `POST /auth/login`, su request, los códigos o los campos de respuesta distintos de los requeridos por el Issue.
- Cambiar el formato o ciclo de vida del token, el esquema de datos, el almacenamiento de contraseñas o la inicialización de la base.
- Refactorizar la separación de capas o corregir otros problemas de autenticación.
- Modificar `api/`, `web/` o componentes generales.

## Contratos protegidos

- `POST /auth/login` conserva la autenticación de `student` / `Workshop123!`: responde `200`, con `user.username` igual a `student` y un token no nulo.
- Una entrada `' OR '1'='1` en el usuario o en la contraseña, con la otra credencial de prueba válida, responde `401` y no devuelve `token`.
- La ruta, el formato de solicitud y la generación de respuesta para el login válido permanecen sin cambios. No se altera el comportamiento de otros casos de validación.

## Componentes afectados

- `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`: única ubicación de la búsqueda de usuario por ambas credenciales.
- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: prueba existente del login exitoso y nuevos casos de rechazo de inyección.
- `AuthResource.login` no requiere cambios para cumplir este alcance: su flujo actual ya devuelve `401` cuando la búsqueda no encuentra usuario.

No se requieren cambios de esquema, dependencias, configuración ni archivos de otros componentes.

## Decisiones

1. **Vincular las dos credenciales como parámetros de la consulta.** Usar una consulta preparada JDBC y enlazar usuario y contraseña por separado mantiene la comparación existente y evita que sus caracteres se interpreten como sintaxis SQL. Se elige frente a escapar manualmente comillas o filtrar caracteres: el escape manual es frágil y el filtrado puede rechazar valores legítimos sin impedir todas las formas de inyección.
2. **Conservar la consulta semántica y el mapeo de usuario.** Mantener las columnas seleccionadas, las condiciones de igualdad y la conversión al mismo `UserRecord` reduce el cambio al límite de autenticación. Reescribir la persistencia o cambiar el esquema no es necesario para los criterios del Issue.
3. **Verificar el contrato a través del endpoint.** Los casos en `AuthResourceTest` ejercitan la frontera HTTP que consume el usuario y cubren el criterio explícito del Issue. Un test aislado del repositorio podría complementar, pero no sustituiría la comprobación de status y ausencia de token.

## Estrategia incremental

1. Mantener el test existente como caracterización del login válido.
2. Antes de cambiar la consulta, agregar a `AuthResourceTest` dos casos de regresión: la cadena de inyección como usuario y como contraseña. Cada caso debe comprobar `401` y que `token` no esté presente. En el estado vulnerable actual, estos casos deben fallar, haciendo visible el comportamiento que se corrige.
3. Cambiar solo la ejecución de la búsqueda en `LegacyDatabase.findByCredentials` para que las dos credenciales se traten como parámetros; conservar el mapeo de resultados y el comportamiento HTTP existente.
4. Ejecutar `mvn test` desde `auth/` y confirmar los tres casos: login válido y ambos rechazos. No se requiere ejecutar ni modificar pruebas de otros componentes para este cambio.

## Riesgos / Trade-offs

- **Un parámetro se omite o se enlaza en posición incorrecta** → cubrir las dos posiciones con escenarios de endpoint y conservar el caso válido.
- **Una excepción de base de datos continúa convirtiéndose en “usuario no encontrado”** → este comportamiento preexistente puede confundir un fallo operativo con `401`; cambiar el manejo de errores queda fuera del Issue y debe tratarse por separado.
- **El contrato Day 1 no está disponible** → antes de implementar, contrastar todos los campos de la respuesta con `docs/ENDPOINTS.md` si se incorpora al checkout; mientras tanto, no modificar los campos que no cubren la spec.
- **Las pruebas dependen del fixture de usuario del entorno Quarkus** → conservar el test actual y reutilizar las credenciales de prueba ya establecidas, sin agregar secretos reales.

## Rollback

No hay migración ni cambio de datos. Si aparece una incompatibilidad del login válido, corregir hacia adelante manteniendo las pruebas de regresión. Revertir la protección de la consulta restauraría la vulnerabilidad; por ello no debe usarse como rollback rutinario. Si una reversión temporal fuera inevitable, requeriría aprobación de seguridad y un plan inmediato para volver a desplegar la corrección.