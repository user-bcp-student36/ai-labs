# Notas de versión (borrador, no publicado)

## Intervalo
- Base: f8a13142367be16fe601d6fb1ac8f16ac8c3205d (cabeza de PR #21, `day2/skill-mcp`, resuelta vía MCP `pull_request_read`). Motivo: la referencia pedida es "PR-21"; se usa la cabeza del PR como punto de partida.
- Head: 5a883dc4c3ab94d6f8e914adb62c9d197b01d2bc (`main` en origin, resuelta vía MCP `list_commits` con `sha=main`).
- Referencia local: `main` local apunta a 1ab2df6e27e3baee16ad62123262f6d3d337a8f7 y está desfasada respecto a `origin/main`. No se modificó.
- Commits en el intervalo `f8a1314..5a883dc`:
  - 5a883dc Merge pull request #21 from user-bcp-student36/day2/skill-mcp

## Cambios
### PR #21 — Day2/skill mcp ([enlace](https://github.com/user-bcp-student36/ai-labs/pull/21))
- Integración: commit de integración 5a883dc, ascendente de `origin/main` y no ascendente de la base f8a1314 (`git merge-base --is-ancestor`). El árbol de 5a883dc es idéntico al de la cabeza f8a1314 (`git diff --stat` vacío), por lo que el merge no añadió cambios propios.
- Contenido integrado (14 archivos, +563 / -14, según `git diff 1ab2df6..5a883dc`):
  - Web: `web/src/app/login/login.component.ts`, `login.component.html`, `login.component.spec.ts`.
  - Configuración de herramientas: `.vscode/mcp.json` (nuevo), `skills-lock.json`.
  - Skills y documentación: `.agents/skills/*` (5 skills), `.github/skills/feature-intake-spec-checklist/` (SKILL.md y ejemplos), `HU-01.md`.
- Efecto:
  - Login web: los mensajes de validación y error ya no se inyectan como HTML (`[innerHTML]`). Ahora se muestran como texto con un titular y un detalle opcionales. Los textos de error cambian de `<strong>...</strong><br>...` a un objeto `{ headline, detail }`.
  - Herramientas y skills del laboratorio: no afectan al runtime del producto.
- Acciones requeridas:
  - Ninguna demostrada para el producto. Si un operador usa `.vscode/mcp.json` del repositorio, debe tener un servidor MCP en `http://localhost:8081/mcp` (evidencia: `.vscode/mcp.json` en 5a883dc). La copia de trabajo local de ese archivo difiere de la integrada; no se verificó cuál es la vigente.
- Cambios incompatibles: no en el contrato de la API (`POST /auth/login`, `/api/tasks`), porque el PR no toca `auth`, `api` ni rutas. Cambio interno de la interfaz web: el componente ya no acepta HTML en los mensajes.
- Commits del PR (según MCP `get_commits`): 9d3c1a8 "Add basic skill: feature intake to spec checklist (v0.1.0)" y f8a1314 "CommitDia2". El PR indica el cuerpo sin rellenar (plantilla); el efecto se derivó del diff.

## Excluidos
- PR #1 a #20 (excepto #21) — no integrados en el intervalo. Sin `merged_at`, con estado abierto o cerrado sin fusión (por ejemplo, #5). PR #20 "Bump pytest from 9.0.2 to 9.1.1 in /api" está abierto.
- Límite de verificación: la lista de PR se obtuvo con `list_pull_requests` (`state: all`, 100 por página, una página). Se confirmó por campo `merged_at` en el listado; no se revisaron páginas adicionales porque el listado devolvió los 20 PR existentes.

## Incertidumbre y evidencia
- Al consultar el listado, el campo `merged` figuraba como `false` para #21, mientras que `pull_request_read get` devolvió `merged: true` y `merged_at: 2026-10-28T14:41:55Z`. Se tomó como válido el detalle del PR y su integración se demostró con git.
- La base se eligió como cabeza de PR #21; con base 1ab2df6 (base de PR #21) el intervalo incluiría también los commits 9d3c1a8 y f8a1314.
- Las pruebas y el CI de PR #21 no se reejecutaron; no se afirma su resultado.
- El tag `skill-feature-intake-v0.1.0` existe en 9d3c1a8; no se creó en esta ejecución.

## Verificación
| Comando | Resultado |
|---|---|
| `git fetch origin` | origin/main actualizado de 1ab2df6 a 5a883dc |
| `git merge-base --is-ancestor 5a883dc origin/main` | True |
| `git merge-base --is-ancestor 5a883dc f8a1314` | False |
| `git merge-base --is-ancestor 5a883dc main` (local) | False |
| `git diff --stat f8a1314 5a883dc` | vacío |
| `git diff --stat 1ab2df6 5a883dc` | 14 files changed, 563 insertions(+), 14 deletions(-) |
| `git cat-file -e 5a883dc...` | existe |
