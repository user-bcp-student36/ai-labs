# Spec Delta

## Purpose

Define cómo el servicio api verifica el token firmado que recibe en `GET /api/tasks` y qué respuesta devuelve cuando el token no es válido.

## ADDED Requirements

### Requirement: Aceptación de tokens firmados válidos
El servicio api SHALL aceptar en `GET /api/tasks` un token cuya firma sea válida, que no esté expirado y que contenga los claims `sub`, `client`, `iat` y `exp`.

#### Scenario: Token válido
- **WHEN** una petición a `GET /api/tasks` incluye un token con firma válida y no expirado
- **THEN** el servicio responde con la lista de tareas en el formato vigente

### Requirement: Rechazo de tokens con firma inválida o alterada
El servicio api SHALL responder HTTP 401 cuando el token tenga una firma que no corresponde a su contenido, incluido un payload o una firma alterados.

#### Scenario: Firma inválida
- **WHEN** una petición a `GET /api/tasks` incluye un token cuya firma no es válida
- **THEN** el servicio responde HTTP 401
- **AND** no devuelve datos de tareas

#### Scenario: Payload alterado
- **WHEN** una petición a `GET /api/tasks` incluye un token cuyo payload fue modificado después de firmarlo
- **THEN** el servicio responde HTTP 401

### Requirement: Rechazo de tokens expirados o malformados
El servicio api SHALL responder HTTP 401 cuando el token esté expirado, no tenga el formato esperado o no contenga los claims requeridos.

#### Scenario: Token expirado
- **WHEN** una petición a `GET /api/tasks` incluye un token cuyo claim `exp` ya pasó
- **THEN** el servicio responde HTTP 401

#### Scenario: Token malformado
- **WHEN** una petición a `GET /api/tasks` incluye un token que no puede interpretarse como token firmado
- **THEN** el servicio responde HTTP 401

#### Scenario: Petición sin token
- **WHEN** una petición a `GET /api/tasks` no incluye el encabezado `Authorization`
- **THEN** el servicio responde HTTP 401

### Requirement: Secreto de verificación desde configuración
El servicio api SHALL obtener el secreto de verificación de la variable de entorno `AUTH_TOKEN_SECRET` y SHALL NOT arrancar si la variable no está definida.

#### Scenario: Arranque sin secreto
- **WHEN** el servicio api se inicia sin la variable `AUTH_TOKEN_SECRET`
- **THEN** el arranque falla con un error que indica la variable faltante
- **AND** el mensaje de error no muestra ningún valor de secreto

### Requirement: Sin exposición de tokens ni secretos en logs
El servicio api SHALL NOT registrar en logs el valor de un token, del secreto de verificación ni del encabezado `Authorization`.

#### Scenario: Validación fallida en logs
- **WHEN** una validación de token falla
- **THEN** los logs registran el motivo del rechazo sin incluir el token ni el secreto
