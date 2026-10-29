# Spec Delta

## Purpose

Define cómo la sesión del cliente web almacena, envía y descarta el token devuelto por el login, sin interpretarlo, para acceder a las rutas protegidas de la aplicación.

## ADDED Requirements

### Requirement: Token almacenado como valor opaco
El cliente web SHALL almacenar el token devuelto por `POST /auth/login` como una cadena opaca y SHALL NOT decodificarlo, interpretarlo ni extraer de él datos de sesión.

#### Scenario: Login exitoso guarda el token sin interpretarlo
- **WHEN** `POST /auth/login` responde con HTTP 200 y un campo `token`
- **THEN** el cliente guarda ese valor exacto como token de sesión
- **AND** no lee claims ni otros datos contenidos en el token

#### Scenario: Respuesta de login sin token
- **WHEN** `POST /auth/login` responde con HTTP 200 sin campo `token`
- **THEN** el cliente no guarda ninguna sesión
- **AND** muestra un mensaje de error y permanece en la pantalla de login

### Requirement: Envío del token como Bearer en rutas protegidas
El cliente web SHALL enviar el token almacenado en el encabezado `Authorization: Bearer <token>` de cada petición a `GET /api/tasks`, sin modificar su contenido.

#### Scenario: Carga del dashboard con sesión activa
- **WHEN** el usuario tiene un token de sesión almacenado y entra al dashboard
- **THEN** la petición a `GET /api/tasks` incluye el encabezado `Authorization: Bearer <token>` con el valor almacenado

#### Scenario: Dashboard sin sesión
- **WHEN** el usuario entra al dashboard sin token de sesión almacenado
- **THEN** el cliente no realiza la petición a `GET /api/tasks`
- **AND** redirige a la pantalla de login

### Requirement: Cierre de sesión ante HTTP 401
El cliente web SHALL eliminar el token y los datos de sesión almacenados cuando `GET /api/tasks` responda HTTP 401, y SHALL redirigir al usuario a la pantalla de login.

#### Scenario: Token rechazado por el backend
- **WHEN** `GET /api/tasks` responde HTTP 401
- **THEN** el cliente elimina el token y los datos de sesión almacenados
- **AND** redirige al usuario a la pantalla de login
- **AND** muestra un mensaje indicando que la sesión ha expirado o no es válida

#### Scenario: Otros errores no cierran la sesión
- **WHEN** `GET /api/tasks` responde con un código distinto de 401, por ejemplo HTTP 500
- **THEN** el cliente conserva el token y los datos de sesión almacenados
- **AND** muestra un mensaje de error en el dashboard

### Requirement: Token ausente del registro visible
El cliente web SHALL NOT escribir el valor del token en la consola, en mensajes de error mostrados al usuario ni en el contenido de la interfaz.

#### Scenario: Error de red durante la carga de tareas
- **WHEN** la petición a `GET /api/tasks` falla por error de red
- **THEN** el cliente muestra un mensaje genérico de error
- **AND** ningún mensaje de consola o interfaz contiene el valor del token
