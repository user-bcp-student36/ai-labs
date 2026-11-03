# Hallazgos

## API

### H-API-01 — Identidad aceptada desde un token sin firma
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:14,17-26`; el token es Base64 decodificado y sus campos se aceptan al coincidir con un secreto fijo incluido en el código. No se verifica una firma criptográfica.
- Impacto: quien conozca el formato y el secreto puede presentar una identidad o rol falsificados ante las rutas protegidas.
- Prioridad propuesta: Alta (P1).
- Contrato potencialmente afectado: autenticación y confianza de la identidad usada por todos los endpoints protegidos.
- Cómo validarlo: agregar pruebas de caracterización para token alterado o no firmado, identidad/rol falsificados y expiración; deben rechazarse tokens que no hayan sido emitidos y firmados por el mecanismo confiable.

### H-API-02 — El parámetro `owner` no está ligado a la identidad autenticada
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:68-70,91-93` usa el `owner` opcional recibido por query en vez de exigir que coincida con `identity["username"]`; `api/app/main.py:77-84` también permite que el body de creación elija el `owner`.
- Impacto: una identidad autenticada puede consultar resúmenes o tareas de otro propietario y crear tareas asignadas a esa persona, sin comprobar permisos.
- Prioridad propuesta: Alta (P1).
- Contrato potencialmente afectado: autorización ligada a la identidad, requerida en `AGENTS.md:41`, y el aislamiento de datos de `/api/tasks` y `/api/summary`.
- Cómo validarlo: probar consultas y creación usando una identidad con un `owner` diferente; verificar que se denieguen o que se aplique una política de roles explícita, sin exponer ni reasignar datos ajenos.

### H-API-03 — Consultas SQL construidas por concatenación
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:70` concatena `requested_owner` en un `SELECT`; `api/app/main.py:84` concatena campos del body en un `INSERT`; `api/app/main.py:93` concatena `requested_owner` en otro `SELECT`.
- Impacto: entradas controladas por el cliente pueden alterar la consulta, eludir filtros o causar errores de persistencia; el `INSERT` también permite manipular su estructura.
- Prioridad propuesta: Alta (P1).
- Contrato potencialmente afectado: consultas parametrizadas requeridas en `AGENTS.md:41` y la integridad/confidencialidad de tareas y resúmenes.
- Cómo validarlo: agregar pruebas para valores con caracteres especiales en filtros y campos de creación; deben tratarse como datos literales y no modificar el conjunto de filas ni la estructura de la consulta.

### H-API-04 — La carga no restringe nombre ni tamaño del archivo
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:107-112` concatena `file.filename` al directorio de cargas, lee el archivo completo en memoria y lo escribe sin límite; no se valida extensión.
- Impacto: un nombre con rutas puede permitir escribir fuera del directorio previsto, y cargas grandes pueden consumir memoria o almacenamiento; tampoco se restringen tipos de archivo.
- Prioridad propuesta: Alta (P1).
- Contrato potencialmente afectado: control de nombre, tamaño y extensión exigido en `AGENTS.md:42`, y el aislamiento del almacenamiento de `/api/requirements`.
- Cómo validarlo: probar nombres absolutos y con segmentos de ruta, extensiones no permitidas y tamaños sobre el límite definido; verificar rechazo, ausencia de escrituras fuera del directorio y respuesta de error consistente.
