# CLAUDE.md

Las reglas del proyecto viven en `AGENTS.md` (fuente única compartida con OpenCode) y se
importan aquí. No dupliques reglas en este archivo: si algo cambia, cámbialo en `AGENTS.md`.

@AGENTS.md
@specs/README.md

## Notas específicas de Claude Code

- Para un requerimiento nuevo o un cambio que toque un contrato, usa el modo de planificación
  y presenta el plan antes de editar.
- Comandos del proyecto (en `.claude/commands/`, iguales a los de OpenCode):
  `/implementar <Rx.y>`, `/revisar <Rx>`, `/registrar-ia <resumen>`, `/sustentacion <Rx>`.
- Después de editar Java, ejecuta `./gradlew test` (incluye `ArquitecturaTest`) antes de dar
  la tarea por terminada.
- Nunca hagas `git commit` ni `git push` sin que el integrante lo pida en ese momento
  (sección 9 de `AGENTS.md`). Opcionalmente el equipo puede reforzarlo con permisos de Claude
  Code: ver "Permisos sugeridos" en `docs/configuracion-ias.md`.
