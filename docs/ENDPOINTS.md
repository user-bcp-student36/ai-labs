# Endpoints

Contratos de los endpoints externos que consume cada componente. Las rutas de código se citan desde la raíz.

Convención por endpoint: contrato observado (lo que el código de web envía y lee), contrato acordado (no hay acuerdo documentado todavía) y riesgos pendientes.

## web

Aplicación Angular. Consume dos endpoints: login de auth y listado de tareas de api. No tiene servicio HTTP propio: ambas llamadas están directamente en componentes con `axios`.

### POST http://localhost:8080/auth/login

**1. Quién lo llama**

- `LoginComponent.login()` en [web/src/app/login/login.component.ts](web/src/app/login/login.component.ts#L41-L45).
- Se dispara al pulsar "Ingresar" en [login.component.html](web/src/app/login/login.component.html).
- Servicio de origen: `auth` (puerto 8080, según la URL hardcodeada).

**2. Qué envía**

Headers: los que pone `axios` por defecto para un cuerpo JSON (`Content-Type: application/json`). La app no añade `Authorization`.

Query params: ninguno.

Body:

| Campo | Tipo | Origen en la UI | Obligatorio | Validación previa en web |
|---|---|---|---|---|
| `username` | string | campo `username` del formulario | sí | no se envía si está vacío o solo tiene espacios (línea 33-35) |
| `password` | string | campo `password` del formulario | sí | no se envía si está vacío o solo tiene espacios (línea 36-38) |
| `client` | string | constante `'angular-workshop'` | sí | — |

Timeout de la petición: 5000 ms ([login.component.ts](web/src/app/login/login.component.ts#L44)).

Ejemplo de request (ilustrativo, con contraseña de laboratorio sustituida):

```json
{
  "username": "student",
  "password": "<contraseña de laboratorio>",
  "client": "angular-workshop"
}
```

**3. Qué espera recibir en éxito**

Código de estado: `200`. La UI solo continúa si `response.data.token` existe ([login.component.ts](web/src/app/login/login.component.ts#L46)).

| Campo | Tipo | Uso en web |
|---|---|---|
| `token` | string | se guarda en `localStorage.workshop_token` ([login.component.ts](web/src/app/login/login.component.ts#L47)) |
| `user` | objeto | se guarda como JSON en `localStorage.workshop_user` ([login.component.ts](web/src/app/login/login.component.ts#L48)); la UI usa `user.username` y `user.displayName` |
| `user.username` | string | se usa en el listado de tareas como parámetro `owner` ([dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L43)) |
| `user.displayName` | string | saludo en el dashboard, con respaldo a `username` |
| `expiresIn` | número | no lo lee web |
| `debug` | string | no lo lee web |

Campo `user.role`: no lo lee web.

Ejemplo de respuesta (ilustrativo; los campos vienen del contrato observado de auth en [PLAN.md](PLAN.md), el valor de `token` es un marcador):

```json
{
  "token": "<token en Base64>",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "<nombre visible>",
    "role": "<rol>"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

Tras el 200 la UI guarda `workshop_token`, `workshop_user` y `workshop_preferences`, y navega a `/dashboard` ([login.component.ts](web/src/app/login/login.component.ts#L46-L50)).

**4. Cómo reacciona ante cada error**

| Caso | Qué hace la UI | Dónde |
|---|---|---|
| Usuario o contraseña vacíos | No llama al backend. Muestra "Ingrese un usuario." o "Ingrese una contraseña." | [login.component.ts](web/src/app/login/login.component.ts#L33-L38) |
| 200 sin `token` | Muestra "El backend respondió sin token." y no guarda nada | [login.component.ts](web/src/app/login/login.component.ts#L52) |
| 400, 401 o 500 (respuesta HTTP de error de axios) | Muestra "No se pudo autenticar a `<usuario>`." seguido de `error.response.data.message`, o si no existe, `error.message` | [login.component.ts](web/src/app/login/login.component.ts#L55-L56) |
| Timeout (más de 5 s) | Muestra "No se pudo autenticar a `<usuario>`." seguido del mensaje de axios (`timeout of 5000ms exceeded`) | mismo bloque |
| Error de red (backend caído) | Igual que timeout, con el mensaje de axios (por ejemplo `Network Error`) | mismo bloque |
| Error inesperado en la respuesta | Igual que error HTTP; además `console.error(error)` | [login.component.ts](web/src/app/login/login.component.ts#L57) |

En todos los casos de error el botón vuelve a estar habilitado (`finally` pone `busy = false`).

Preguntas abiertas:
- El nombre del campo del cuerpo de error (`message`) no está verificado desde web: web lo lee, pero el contrato de auth no lo documenta. Si auth usa otra clave, el mensaje mostrado será el genérico de axios.
- El contrato de error de auth registrado en [PLAN.md](PLAN.md) no indica el nombre de la clave del cuerpo de error.

**5. Notas de seguridad**

- El token y los datos de usuario se guardan en `localStorage` desde `LoginComponent` (línea 47-48). El reto lo marca como deuda técnica: lo que se almacena es legible por cualquier script de la misma origin.
- El mensaje de error se renderiza con `[innerHTML]` ([login.component.html](web/src/app/login/login.component.html#L39)) y concatena `this.username` sin escapar ([login.component.ts](web/src/app/login/login.component.ts#L56)). Angular sanitiza `innerHTML` por defecto, pero el reto y las buenas prácticas piden no renderizar datos dinámicos como HTML. Riesgo pendiente.
- La URL del backend está hardcodeada en el componente ([login.component.ts](web/src/app/login/login.component.ts#L42)). Las buenas prácticas piden `environment.ts`.

### GET http://localhost:8000/api/tasks?owner=`<usuario>`

**1. Quién lo llama**

- `DashboardComponent.ngOnInit()` en [web/src/app/dashboard/dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L43-L46).
- Se ejecuta al entrar en `/dashboard` ([app.routes.ts](web/src/app/app.routes.ts)).
- Servicio de origen: `api` (puerto 8000, según la URL hardcodeada).

**2. Qué envía**

Headers:

| Header | Valor | Origen |
|---|---|---|
| `Authorization` | `Bearer <token>` | `localStorage.workshop_token` ([dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L36)) |

Query params:

| Parámetro | Tipo | Origen | Obligatorio |
|---|---|---|---|
| `owner` | string | `user.username` de `localStorage.workshop_user`; si no existe, cadena vacía | sí, según el código (no hay validación) |

Body: ninguno.

Ejemplo de request (ilustrativo):

```
GET http://localhost:8000/api/tasks?owner=student
Authorization: Bearer <token>
```

Nota: `owner` lo decide el cliente. Eso tiene relación con el hallazgo de autorización de api (el reto lo lista como "El cliente puede consultar otro `owner`"); se documenta aquí solo como comportamiento de web.

**3. Qué espera recibir en éxito**

Código de estado: `200`. El cuerpo se usa como lista (`response.data`) y se recorre con un bucle `for` ([dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L47)).

Cada elemento de la lista debe tener estos campos:

| Campo | Tipo | Uso en web |
|---|---|---|
| `title` | string | título de la tarjeta |
| `description` | string | texto de la tarjeta |
| `status` | string | se oculta si vale `ARCHIVED`; se muestra con etiqueta si está en `TODO`, `IN_PROGRESS`, `DONE`, `RESUELTO` o `COMPLETED`; otro valor se muestra tal cual |
| `priority` | string | se usa para ordenar y en la etiqueta (`formatTaskLabel`) |
| `owner` | string | se usa en la etiqueta (`formatTaskLabel`) |

El campo `label` lo calcula web, no viene del backend ([legacy-format.ts](web/src/app/utils/legacy-format.ts)).

Ejemplo de respuesta (ilustrativo; no verificado contra el backend de api, solo con los campos que lee web):

```json
[
  {
    "title": "Revisar requerimiento",
    "description": "<descripción>",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "owner": "student"
  }
]
```

Preguntas abiertas:
- El backend puede devolver otros campos (por ejemplo `id`). Web no los usa; no se documentan porque no están verificados desde web.
- Valores posibles de `priority`: web no los define. La ordenación compara strings, así que el orden depende de los valores reales.
- Si el backend devuelve algo distinto de una lista: si `response.data` es `null`, se produce un error que cae en el bloque de error; si es un objeto sin `length`, la lista queda vacía.

**4. Cómo reacciona ante cada error**

| Caso | Qué hace la UI | Dónde |
|---|---|---|
| Sin token en `localStorage` | No llama al backend; redirige a `/login` | [dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L38-L40) |
| Token inválido o expirado (respuesta de error HTTP, por ejemplo 401) | No redirige a login. Muestra `error.response.data.detail` o, si no existe, `error.message` (por ejemplo `Request failed with status code 401`). La lista se oculta | [dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L66-L69) |
| Timeout | No hay timeout configurado en esta petición (axios por defecto no tiene límite). La UI se queda mostrando el esqueleto de carga hasta que la petición responda o falle por red | [dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L43-L46) |
| Error de red | Muestra `error.message` | mismo bloque |
| Respuesta inesperada (no lista) | Ver preguntas abiertas: lista vacía o mensaje de error | [dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L47-L56) |

Preguntas abiertas:
- El nombre del campo `detail` en el cuerpo de error no está verificado desde web. Viene de la convención de FastAPI que el código de web asume; api no está en el alcance de este documento.
- Caducidad del token: web no lee `expiresIn` del login ni comprueba la expiración local. Un token vencido se detecta solo cuando api responde con error.

**5. Notas de seguridad**

- Token en `localStorage` (leído en [dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L36)). Lo escribe `LoginComponent` y lo borra `logout()` ([dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L74-L77)).
- `owner` se toma de `localStorage` y lo puede modificar el usuario. Web no verifica que coincida con la identidad del token.
- La URL del backend está hardcodeada ([dashboard.component.ts](web/src/app/dashboard/dashboard.component.ts#L43)).
- El texto de tareas se muestra con interpolación (`{{ }}`), no con `[innerHTML]`. La interpolación escapa el contenido por defecto.

## Resumen de dependencias de web

| Endpoint | Componente | Riesgo principal pendiente |
|---|---|---|
| `POST http://localhost:8080/auth/login` | `LoginComponent` | `[innerHTML]` con datos dinámicos; token en `localStorage` |
| `GET http://localhost:8000/api/tasks` | `DashboardComponent` | `owner` decidido por el cliente; sin timeout; sin redirección ante 401 |

## Pendientes

- Confirmar el nombre de las claves de error de auth (`message`) y de api (`detail`).
- Confirmar el esquema completo de `GET /api/tasks` y los valores de `priority` y `status` con el equipo de api.
- Auth y api no tienen sección en este documento; se añadirán al cerrar sus componentes.
