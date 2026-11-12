---
description: Implementa un requerimiento siguiendo su spec (SDD) y las reglas de AGENTS.md
---

Vas a implementar **$ARGUMENTS** del proyecto "Rutas del Paisaje Cultural Cafetero"
(Java 21 + Spring Boot, arquitectura en 3 capas).

1. Lee `AGENTS.md` (secciones 2 a 5) y la spec correspondiente en `specs/`. Cita los IDs de
   comportamiento y de casos de prueba (por ejemplo R2.1, T2-b) que vas a cubrir.
2. Presenta un plan breve: clases y capa (`controller`, `cli`, `model`,
   `algorithm`, `service`, `repository`, o `python/` si la spec lo indica),
   métodos y casos de prueba. Si el cambio altera un contrato (firma pública, columnas,
   formato), propón primero el cambio en la spec y espera mi aprobación.
3. Si la spec tiene casos de prueba que aún no están en `src/test/java`, escríbelos antes del
   código.
4. Implementa. En `algorithm`: solo `java.lang`/`java.util`, métodos `static`, Javadoc
   con spec, idea, complejidad en tiempo y memoria, fuente (o `[FUENTE PENDIENTE]`) y
   recurrencia si aplica. Sin `System.out` fuera de `controller`/`cli`, salvo el canal de `ProcesoAislado`.
5. Ejecuta `./gradlew test` (al menos la prueba del requerimiento y `ArquitecturaTest`). No
   digas que terminaste con pruebas en rojo.
6. Explícame en 5–10 líneas la idea, el invariante o la recurrencia y la complejidad, como
   para responder en la sustentación.
7. Agrega la entrada correspondiente en `docs/registro-ia.md` y actualiza el **Estado** de la
   spec. No hagas commit.
