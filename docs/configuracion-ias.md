# Configuración de Claude Code y OpenCode

## Qué lee cada herramienta

| Archivo | Claude Code | OpenCode |
|---|---|---|
| `AGENTS.md` | Sí, porque `CLAUDE.md` lo importa con `@AGENTS.md` | Sí (archivo de reglas del proyecto) |
| `CLAUDE.md` | Sí | No, porque existe `AGENTS.md` y OpenCode usa solo ese |
| `specs/README.md` | Importado en `CLAUDE.md` | Cargado por `opencode.json` → `instructions` |
| Comandos | `.claude/commands/*.md` | `.opencode/commands/*.md` |

Los dos reciben las mismas reglas. **Las reglas se editan solo en `AGENTS.md`.**

## Comandos disponibles (iguales en ambas)

| Comando | Para qué |
|---|---|
| `/implementar R2.1` | Implementar un requerimiento siguiendo su spec (plan → pruebas → código → explicación → registro) |
| `/revisar R3` | Revisión sin cambios contra la spec, la implementación propia y el protocolo |
| `/registrar-ia <resumen>` | Agregar la fila del anexo de IA |
| `/sustentacion R4` | Simulacro de preguntas del docente, una a la vez |

Si una versión antigua de OpenCode no encuentra los comandos, renombrar la carpeta a
`.opencode/command/` (singular), que era el nombre anterior.

## Permisos sugeridos para Claude Code (opcional)

Para que Claude Code pida confirmación antes de hacer commit o push, cada integrante puede crear
`.claude/settings.json` (versionado) o `.claude/settings.local.json` (personal) con:

```json
{
  "permissions": {
    "allow": ["Bash(./gradlew test:*)", "Bash(./gradlew bootRun:*)",
              "Bash(python -m pytest:*)", "Bash(python -m preparacion:*)"],
    "ask": ["Bash(git commit:*)", "Bash(git push:*)"]
  }
}
```

## Buenas prácticas del equipo

- Una sesión de IA por tarea y por requerimiento; al empezar, indicar el ID (`R2.1`).
- Revisar siempre el diff antes de aceptar. Quien hace el commit debe entender cada línea.
- Alternar herramientas para revisar: lo que implementó una (p. ej. Claude Code) lo revisa la
  otra con `/revisar` (p. ej. OpenCode), y luego un integrante.
- Antes de la sustentación, cada integrante hace `/sustentacion` sobre los requerimientos
  que **no** implementó.
