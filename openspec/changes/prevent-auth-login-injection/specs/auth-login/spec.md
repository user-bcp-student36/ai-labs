# Spec Delta

## Purpose

Define el contrato observable del login para las credenciales válidas del taller y asegura que los valores de credenciales manipulados no concedan acceso.

## ADDED Requirements

### Requirement: Successful login preserves the existing contract
El sistema MUST continuar aceptando las credenciales existentes del taller mediante `POST /auth/login` y devolver una respuesta exitosa con el nombre de usuario autenticado y un token no nulo.

#### Scenario: Inicio de sesión del usuario existente del taller
- **WHEN** a client submits `student` and `Workshop123!` to `POST /auth/login`
- **THEN** the response status is `200`, `user.username` is `student`, and `token` is not null

### Requirement: Manipulated credentials cannot grant access
El sistema MUST tratar los valores de usuario o contraseña con forma de inyección como credenciales inválidas y MUST NOT autenticar la solicitud debido a su contenido.

#### Scenario: Se rechaza un usuario con forma de inyección
- **WHEN** un cliente envía `' OR '1'='1` como usuario y `Workshop123!` como contraseña a `POST /auth/login`
- **THEN** el estado de respuesta es `401` y la respuesta no contiene `token`

#### Scenario: Se rechaza una contraseña con forma de inyección
- **WHEN** un cliente envía `student` como usuario y `' OR '1'='1` como contraseña a `POST /auth/login`
- **THEN** el estado de respuesta es `401` y la respuesta no contiene `token`