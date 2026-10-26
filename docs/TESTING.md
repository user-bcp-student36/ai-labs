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

## web

Comando: `npx vitest run --coverage` (cwd: `web/`). Instalación previa: `npm ci` en `web/`, exit code 0.

Resultado real: 3 archivos de prueba, 22 pruebas, 22 aprobadas, 0 fallos. Exit code 0.

### Tabla de casos

| Flujo | Caso | Tipo | Resultado esperado |
|---|---|---|---|
| Login | Credenciales válidas guardan token, usuario y preferencias y navegan a /dashboard | Camino feliz | POST a `/auth/login` con timeout 5000; `workshop_token` guardado; navega a `['/dashboard']` |
| Login | Usuario vacío o solo espacios | Caso borde | Mensaje "Ingrese un usuario."; sin llamada HTTP |
| Login | Contraseña vacía | Caso borde | Mensaje "Ingrese una contraseña."; sin llamada HTTP |
| Login | Respuesta 200 sin token | Caso borde | Mensaje "El backend respondió sin token."; sin token guardado; sin navegación |
| Login | Preferencias guardadas se combinan con las por defecto | Caso borde | `preferences` contiene el valor guardado y los demás por defecto |
| Login | JSON de preferencias corrupto | Caso borde | Se conservan las preferencias por defecto; no lanza error |
| Login | Servicio de autenticación no disponible (error de red) | Error | Mensaje "No se pudo autenticar a ana." + "Network Error"; sin token; `busy` en false |
| Login | Backend responde 503 con cuerpo | Error | Mensaje con `response.data.message` del backend |
| Login | Timeout de 5000 ms | Error | Mensaje con "timeout of 5000ms exceeded"; sin navegación |
| Tareas | Carga con token, oculta ARCHIVED y etiqueta visibles | Camino feliz | GET a `/api/tasks?owner=ana` con `Authorization: Bearer <token>`; etiqueta "HIGH · ANA"; `loading` en false |
| Tareas | Traducción de estado conocido | Camino feliz | `todo` muestra "Por hacer"; `IN_PROGRESS` muestra "En progreso" |
| Sesión | Logout limpia token y usuario y navega a /login | Camino feliz | `workshop_token` y `workshop_user` eliminados; navega a `['/login']` |
| Tareas | Sin token | Caso borde | Redirige a /login; sin llamada HTTP |
| Tareas | Lista vacía del backend | Caso borde | `visibleTasks` vacío; sin error |
| Tareas | Todas las tareas ARCHIVED | Caso borde | `visibleTasks` vacío; sin error |
| Tareas | Estado desconocido o vacío | Caso borde | Devuelve el estado original (`BLOQUEADA`, cadena vacía) |
| Tareas | Token inválido (401) | Error | `error` muestra `detail` del backend ("Token inválido"); `loading` en false |
| Tareas | Servicio no disponible (error de red) | Error | `error` muestra "Network Error" |
| Tareas | Timeout recibido de axios | Error | `error` muestra "timeout of 5000ms exceeded"; `loading` en false |
| Utilidad | Tarea nula | Caso borde | Devuelve "SIN-DATOS" |
| Utilidad | Prioridad y dueño vacíos | Caso borde | Devuelve "NORMAL · SIN-ASIGNAR" |
| Utilidad | Formato prioridad · dueño | Camino feliz | "HIGH · STUDENT" (prueba existente) |

Archivos: `web/src/app/login/login.component.spec.ts` (9 pruebas), `web/src/app/dashboard/dashboard.component.spec.ts` (10 pruebas), `web/src/app/utils/legacy-format.spec.ts` (3 pruebas; se añadieron 2 casos borde). Auxiliar: `web/src/testing/local-storage.stub.ts`.

Método de ejecución: los componentes se instancian directamente con `Router`, `ChangeDetectorRef` y `NgZone` simulados; `axios` se simula con `vi.mock`; `localStorage` se reemplaza por un almacén en memoria. Entorno de Vitest: `node` (sin DOM, sin TestBed). Se importa `@angular/compiler` para cargar el compilador JIT requerido por `@angular/common`.

### Cobertura

Método: líneas cubiertas / líneas totales, leídas de `web/coverage/coverage-summary.json` (proveedor v8).

Código probado (no se modificó código de producción en este paso; la meta de 85% se evalúa sobre los archivos ejercitados por estas pruebas):

| Archivo | Cubiertas / totales | % líneas | % ramas |
|---|---|---|---|
| web/src/app/login/login.component.ts | 60 / 60 | 100% | 93.33% |
| web/src/app/dashboard/dashboard.component.ts | 68 / 68 | 100% | 80.95% |
| web/src/app/utils/legacy-format.ts | 8 / 8 | 100% | 100% |

Global del reporte (All files): 154 / 186 líneas, 82.79%. Se registra por separado; no alcanza 85% porque incluye archivos sin prueba.

### Fallos

Ninguno en la última ejecución. Durante el desarrollo, las suites de componentes fallaron al cargar por falta de `@angular/compiler`; se corrigió importándolo en la especificación. No se modificaron aserciones para pasar.

### Hallazgos (no cubiertos por pruebas aprobadas; reportados por separado)

1. Dashboard: la petición `GET /api/tasks` no tiene timeout (`dashboard.component.ts`, líneas 43-46). Un backend colgado deja la pantalla en carga indefinida. Se probó solo el mensaje de error de timeout, no la ausencia de timeout.
2. Dashboard: un 401 en tareas muestra el error pero no redirige a /login (`dashboard.component.ts`, bloque catch). La prueba de 401 verifica solo el mensaje; no fija la ausencia de redirección.
3. Login: el usuario se concatena en `innerHTML` sin escapar (`login.component.ts`, línea 56; `login.component.html`, línea 39). Riesgo de inyección HTML.
4. Dashboard: el comparador de orden `a.priority > b.priority ? -1 : 1` no es consistente para prioridades iguales ni para orden alfabético de prioridades. No se prueba el orden.
5. Dashboard: si `response.data` no es un arreglo, el bucle puede fallar; no se prueba.
6. Dashboard: el `owner` viene de `localStorage` (controlado por el cliente), según docs/ENDPOINTS.md.

### Pruebas propuestas (no implementadas)

- Login: nombre de usuario con HTML (`<img onerror=...>`) debe mostrarse escapado; hoy fallaría.
- Tareas: timeout explícito en la petición de tareas; hoy no existe.
- Tareas: 401 debe limpiar sesión y redirigir a /login; hoy no ocurre.
- Tareas: respuesta no arreglo debe mostrar error controlado.
- Login: respuesta con token y sin `user` (verificar el manejo de `JSON.stringify(undefined)`).

### Pendientes

- Confirmar en el backend de `api` las claves de error (`detail`) y el esquema de tareas (docs/ENDPOINTS.md).
- Confirmar la clave `message` del error de `auth`.
- `npm ci` reportó 25 vulnerabilidades (npm audit); no se evaluaron en este paso.
- La sección `auth` indica 25 pruebas, pero la última ejecución registrada en esta sesión fue de 26 pruebas; no se actualizó en este paso.
- Pruebas de DOM (plantillas, `[innerHTML]`) no existen: el entorno es `node`.
