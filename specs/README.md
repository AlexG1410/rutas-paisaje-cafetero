# Especificaciones (Spec-Driven Development)

El enunciado (sección 3) promueve SDD: **antes de programar** cada requerimiento se escribe
una especificación breve con su comportamiento esperado y sus casos de prueba. Estas specs
son el contrato que siguen las personas del equipo, Claude Code y OpenCode.

| Archivo | Requerimiento | Peso |
|---|---|---|
| [protocolo-experimental.md](protocolo-experimental.md) | Sección 6 (aplica a R1–R4) | — |
| [R1-modelo-red.md](R1-modelo-red.md) | Modelo de la red vial | 15 % |
| [R2-caminos-minimos.md](R2-caminos-minimos.md) | Dijkstra lineal y con cola + matriz | 20 % |
| [R3-envolvente-convexa.md](R3-envolvente-convexa.md) | Envolvente convexa (divide y vencerás) | 20 % |
| [R4-ruta-optima.md](R4-ruta-optima.md) | Fuerza bruta, Held-Karp, heurística, itinerario | 25 % |
| [R5-aplicacion.md](R5-aplicacion.md) | Aplicación demostrativa | 5 % |

En R1–R4 la nota se reparte: **40 %** implementación correcta y verificada, **30 %** análisis
teórico (complejidad y correctitud), **30 %** análisis experimental. Cada spec tiene una
sección para cada una de las tres partes.

## Flujo de trabajo por requerimiento

### Estado actual del bloque R3 (2026-11-12)

QuickHull propio, pruebas, exportación para R5, cuatro conjuntos verificados/medidos
contra SciPy, figura y documento disponibles. Python 166/166; suite Java completa
86/93 aprobadas, con 7 fallos por R4 pendiente. R1–R3 y arquitectura pasan. Las líneas
base de R2 y del entorno que siguen son históricas, no el estado actual.

### Estado actual del bloque R2 (2026-11-12)

Implementación Java, pruebas, 30 consultas medidas, matriz 40×40, verificación NetworkX
(30 rutas/costos y 1600 celdas), figura y análisis disponibles. Python 142/142; suite
Java completa 70/84 aprobadas, con 14 fallos por R3/R4 pendientes. La arquitectura pasa.
Las líneas base posteriores son históricas. Contratos y evidencia detallada en R2.

### Organización Java (actualizada el 2026-11-10)

Los paquetes usan nombres en inglés: `controller`, `cli`, `service` (con
`service.measurement`), `repository`, `model` y `algorithm`. Las pruebas reflejan esos
paquetes; las reglas de ArchUnit residen en `architecture`.
Se conservan las tres capas y sus restricciones: presentación (`controller`, `cli`) →
lógica (`service`, `algorithm`, `model`) → datos (`repository`). Esta reorganización no
cambia métodos, endpoints, formatos de archivos ni el estado funcional de R1–R5.

### Línea base de verificación del entorno (2026-11-10, antes de implementar R1.4)

- Java: compilación de producción y pruebas confirmada con Corretto 21.0.10 y Gradle 9.7.1.
- Suite completa: 54 pruebas, 27 aprobadas y 27 fallidas por `UnsupportedOperationException`
  en los algoritmos pendientes de R1–R4. No se omitieron ni deshabilitaron pruebas.
- Ejecución separada de infraestructura: 26/26 aprobadas (`ArquitecturaTest`,
  `RepositoriosTest`, `GrafoTest`, `CatalogoServiceTest` y `MedidorTest`), incluidas las seis
  reglas de ArchUnit y las pruebas con JVM hija.
- Python: entorno local `python/.venv` con Python 3.14.3 y todas las dependencias de
  `requirements.txt` instaladas. `pip check` no reporta incompatibilidades; las diez
  dependencias directas se importan correctamente y `pytest tests -v` aprueba 5/5 pruebas
  de medición, información de máquina y proyección UTM. OSMnx 2.1.1 y SciPy 1.18.1.
- Los requerimientos conservan su estado de borrador: comprobar el entorno no equivale a
  implementar ni verificar los algoritmos.

Avance posterior del 2026-11-10: búsqueda de R1.4 implementada, 6/6 pruebas aprobadas;
infraestructura más R1.4, 32/32 aprobadas. Suite completa: 56 pruebas, 33 aprobadas y 23
fallidas por algoritmos pendientes de R2–R4. R1 sigue parcial; ver su spec.

Descarga de R1 (2026-11-10): implementación y persistencia GraphML verificadas sin red,
15 comprobaciones nuevas; suite Python 20/20. Adquisición real completada, GraphML releído
y reutilización comprobada sin peticiones ni cambios de contenido o fecha de modificación.
Los archivos procesados y los experimentos de R1 siguen pendientes.

R1.2: política de velocidades aprobada e implementada; 36 comprobaciones nuevas y suite
Python 56/56. Red real validada en memoria sin modificar GraphML. Falta integrar pesos
con la selección de la componente fuerte y la exportación de la red.

R1.3/R1.5: integración de procesamiento implementada y ejecutada; CSV de componente
validados con Kosaraju y leídos por Java. Suite Python 65/65, incluida exportación y UTM.
Catálogo sin coordenadas: lugares UTM vacíos (solo cabecera); no se declara R1 completo.
Medición de SCC y experimentos siguen pendientes. Algoritmo documentado según NetworkX 3.7.

### Pasos

1. **Spec**: leer/ajustar la spec. Si cambia un contrato (firma, columnas de un CSV,
   formato de archivo), se cambia **primero aquí** y luego en el código.
2. **Pruebas**: los casos de la tabla "Casos de prueba" se escriben en `src/test/java` con el
   mismo identificador (por ejemplo `t2a_distanciasDeLaGuia`). Ya existen pruebas para los
   casos base.
3. **Implementación**: en `src/main/java/co/edu/uniquindio/rutas/...` (capa y clase que
   indica la spec), respetando las reglas de `AGENTS.md`.
4. **Verificación**: `./gradlew test` en verde (incluye `ArquitecturaTest`) + la verificación contra otra implementación o
   librería que pide la spec (protocolo 6.6). Un algoritmo rápido pero incorrecto no cuenta.
5. **Medición**: `./gradlew bootRun --args='rN'` (y los pasos de `python/` que indique la
   spec) escriben los CSV de `results/rN/`.
6. **Reporte**: figuras con `python -m preparacion graficas` y redacción en
   `docs/documento-tecnico/`.

Al terminar cada paso se actualiza el campo **Estado** de la spec:
`borrador → aprobada → implementada → verificada → medida → documentada`.

## Plantilla para una spec nueva

```markdown
# Rx.y — Nombre
Estado: borrador · Responsable: <integrante>

## Objetivo
(qué pide el enunciado, citado o parafraseado)

## Contrato
(firmas de funciones, archivos que lee y escribe, columnas)

## Comportamiento esperado
(reglas numeradas, incluidos casos borde)

## Restricciones
(implementación propia, librerías permitidas solo para verificar, etc.)

## Casos de prueba
| ID | Entrada | Salida esperada | Origen del valor esperado |

## Criterios de aceptación
- [ ] ...

## Análisis teórico (va al documento técnico)
(complejidad en tiempo y memoria + fuente; recurrencia; argumento de correctitud)

## Experimento
(qué se mide, cuántas veces, archivo de salida, figura)

## Decisiones abiertas
(lo que el equipo debe decidir y justificar)
```
