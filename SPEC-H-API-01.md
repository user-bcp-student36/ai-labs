# Identidad aceptada desde un token sin firma

## Fuente

Hallazgo seleccionado por el usuario como fuente aprobada: `H-API-01 — Identidad aceptada desde un token sin firma`. `HALLAZGOS.md` conserva la etiqueta previa `CANDIDATO`; esta spec no modifica ese registro.

Evidencia: `api/app/main.py:14,17-26` decodifica el token como Base64, interpreta sus campos y acepta la identidad si coinciden con un secreto fijo incluido en el código. No verifica una firma criptográfica.

Impacto: quien conozca el formato y el secreto puede presentar una identidad o un rol falsificados ante las rutas protegidas.

## Contexto actual

La dependencia de autenticación de la API interpreta un token enviado en `Authorization`, comprueba sus campos y su expiración, y entrega `username` y `role` a las rutas protegidas. La identidad se obtiene de datos que no están protegidos por una firma verificable. Las rutas protegidas incluyen `/api/tasks`, `/api/summary`, `/api/requirements` y `/api/backend-health`.

## Problema confirmado

La API no puede determinar que la identidad y el rol del token provienen de un emisor confiable ni que permanecieron íntegros. Por ello, un token con campos manipulados puede aceptarse como identidad autenticada.

## Comportamiento a preservar

- Los endpoints y formatos de respuesta de la API utilizados por el frontend se mantienen.
- Las rutas protegidas siguen requiriendo autenticación; las credenciales inválidas o expiradas se rechazan con HTTP 401.
- La identidad usada por la API debe provenir de credenciales cuya autenticidad e integridad hayan sido verificadas.
- Si el cambio requiere coordinar el contrato con el servicio de autenticación, no se modifica implícitamente el contrato de `POST /auth/login` (`token`, `expiresIn` y `user`).

## Alcance

- Establecer los requisitos observables para aceptar o rechazar tokens en las rutas protegidas de la API.
- Verificar que la identidad y el rol aceptados no puedan ser alterados sin que la autenticación sea rechazada.
- Cubrir los casos de token válido, no verificable, manipulado y expirado.

## Contrato HMAC para pruebas

- El token usa el formato compacto JWT de tres segmentos: `header.payload.signature`.
- `header` es JSON UTF-8 codificado en Base64URL sin padding, con `alg: HS256` y `typ: JWT`.
- `payload` es JSON UTF-8 codificado en Base64URL sin padding, con `username` y `role` de tipo string y `exp` como segundos Unix enteros.
- La entrada firmada es el texto ASCII exacto `header.payload`; `signature` es HMAC-SHA-256 codificado en Base64URL sin padding.
- La API obtiene la clave de `WORKSHOP_TOKEN_HMAC_SECRET`. Las pruebas usan una clave sintética aislada; no se incluye una clave real ni de producción.

## Fuera de alcance

- Modificar el emisor de tokens en `auth`; se asume que ya emite el contrato HMAC descrito para las pruebas.
- Cambiar la política de permisos por rol o la autorización sobre recursos.
- Cambiar endpoints, formatos de respuesta o funcionalidades de la API ajenas a la validación del token.
- Cambiar el flujo de login o el contrato de respuesta de `POST /auth/login`.

## Criterios de aceptación

1. Un token cuya autenticidad e integridad no puedan verificarse se rechaza con HTTP 401 en las rutas protegidas, aunque sus campos sean legibles y tengan el formato esperado.
2. Alterar la identidad o el rol de un token válido invalida la autenticación; esos valores no se aceptan como identidad verificada.
3. Un token vencido se rechaza con HTTP 401.
4. Un token válido que cumpla el contrato JWT HS256 permite acceder a las rutas protegidas y conserva sus respuestas exitosas existentes.
5. Las pruebas cubren tokens válidos, no verificables, manipulados y vencidos, incluyendo el rechazo antes de ejecutar la operación protegida.
6. La validación no depende de secretos reales ni de un secreto de producción incluido en el código.

## Contratos afectados/protegidos

- API (`AGENTS.md`, sección `api`): conservar endpoints y formatos usados por el frontend.
- API (`AGENTS.md`, sección `api`): mantener la autenticación y la identidad ligadas a una fuente verificable.
- Autenticación (`AGENTS.md`, sección `auth`): conservar el contrato de `POST /auth/login`; cualquier ajuste requerido para interoperar con la API debe acordarse explícitamente.
- API: la clave HMAC se configura mediante `WORKSHOP_TOKEN_HMAC_SECRET` y no se incluye en el código.
- No introducir secretos reales ni valores de producción.

## Riesgos, supuestos y preguntas abiertas

- Debe confirmarse que el emisor actual de `auth` usa exactamente el contrato JWT HS256 definido aquí; el emisor no se modificó ni se inspeccionó en este alcance.
- El despliegue debe configurar `WORKSHOP_TOKEN_HMAC_SECRET` con la misma clave protegida que usa el emisor; si falta, la API rechaza solicitudes con HTTP 503.
- Debe acordarse la interoperabilidad entre los tokens del servicio de autenticación y los que acepta la API, sin asumir que los tokens legados sin firma deben seguir siendo aceptados.
- Esta spec trata la autenticidad e integridad de identidad y rol; no define permisos asociados a cada rol.
- El registro de aprobación de H-API-01 en `HALLAZGOS.md` debe reconciliarse con la aprobación indicada para esta spec.
