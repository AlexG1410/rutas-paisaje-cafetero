# AGENTS.md — Reglas para agentes de IA (OpenCode, Claude Code y cualquier otro)

Este archivo es la **fuente única** de reglas del proyecto. `CLAUDE.md` lo importa; OpenCode lo
lee directamente. Si una regla cambia, se cambia **aquí** y en ningún otro lugar.

## 1. Contexto

Proyecto final de **Análisis de Algoritmos** (Ingeniería de Sistemas, Universidad del Quindío,
2026-2): *Rutas del Paisaje Cultural Cafetero*. Planificación de rutas sobre la red vial real
del Quindío (OpenStreetMap) y análisis **teórico y experimental** de sus algoritmos.

- Enunciado y anexo de formatos: `docs/enunciado/` (versión `.txt` si no puedes leer PDF).
- Guías del docente (notación del curso): `docs/guias/` (local, no se versiona).
- Specs (contrato de cada requerimiento): `specs/`. **Son la autoridad para el código.**

| Req. | Qué | Peso | Spec |
|---|---|---|---|
| R1 | Red vial: representación, pesos, SCC, lugares → nodos, guardado | 15 % | `specs/R1-modelo-red.md` |
| R2 | Dijkstra lineal O(V²) y con cola O((V+E) log V); matriz de tiempos | 20 % | `specs/R2-caminos-minimos.md` |
| R3 | Envolvente convexa por divide y vencerás vs SciPy | 20 % | `specs/R3-envolvente-convexa.md` |
| R4 | TSP asimétrico: fuerza bruta, Held-Karp, heurística voraz, itinerario 11 h | 25 % | `specs/R4-ruta-optima.md` |
| R5 | Aplicación demostrativa (Spring Boot REST + Leaflet) | 5 % | `specs/R5-aplicacion.md` |
| — | Documento técnico | 15 % | `docs/documento-tecnico/` |

En R1–R4: 40 % implementación **correcta y verificada**, 30 % análisis teórico, 30 % análisis
experimental. La nota individual se multiplica por un factor de sustentación (0 a 1): **cada
integrante debe poder explicar y modificar cualquier parte del código.**

## 2. Tecnología y reparto Java / Python

- **Java 21 + Spring Boot 4.1 + Gradle** (wrapper `./gradlew`): TODOS los algoritmos, los
  experimentos de R1.1, R1.4, R2, R3 y R4, y la aplicación (R5). Un solo lenguaje para los
  algoritmos, como exige el enunciado.
- **Python (`python/`)**, solo lo que el enunciado exige o que solo existe en Python: descargar
  la red con OSMnx, pesos, componente fuertemente conexa (`ox.truncate.largest_component`) y su
  medición (R1.3), exportar a CSV, verificar contra NetworkX / OSMnx / SciPy, medir SciPy (R3)
  y generar las figuras con Matplotlib. **Ningún algoritmo propio se escribe en Python.**
- El intercambio es por archivos (el enunciado lo permite: "el grafo y el catálogo se exportan
  a archivos y se leen desde allí"): `data/processed/*.csv(.gz)` y `results/**/*.csv`.

## 3. Arquitectura en 3 capas y mapa del repositorio

El equipo trabaja **siempre por capas**. Paquete base `co.edu.uniquindio.rutas`; las
dependencias van en una sola dirección:

```
controller / cli  ──►  service  ──►  repository
                         │
                         ▼
                     algorithm ──► model
(Constantes y RutasApplication: transversales)
```

| Capa | Paquete | Responsabilidad | Spring |
|---|---|---|---|
| Presentación | `controller`, `cli` | Controladores REST (R5), manejo de errores HTTP, comandos `bootRun --args`. Única capa que imprime. | `@RestController`, `@Component` |
| Lógica | `model` | `Grafo`, `Arista`, `Punto`, `Lugar`, resultados (records). Puro. | ninguna |
| Lógica | `algorithm` | Implementación PROPIA: `Dijkstra`, `EnvolventeConvexa`, `TspFuerzaBruta`, `HeldKarp`, `VecinoMasCercano`, `NodoMasCercano`, `Tsp`. Puro, métodos `static`. | ninguna |
| Lógica | `service` | Casos de uso: `RedService`, `CatalogoService`, `RutaService`, `ItinerarioService`, `ExperimentoR1..R4Service`, `measurement.Medidor`. | `@Service` |
| Datos | `repository` | Repositorios de archivos: `RedRepository`, `CatalogoRepository`, `LugaresNodosRepository`, `MatrizRepository`, `ResultadosRepository`, `MaquinaRepository`, `Csv`. | `@Repository` |

Reglas de capas (las verifica `ArquitecturaTest` con **ArchUnit**):
- `repository` no conoce a `service`, `algorithm`, `model`, `controller` ni `cli`: entrega records propios (`RedLeida`,
  `FilaCatalogo`...) y la lógica arma sus objetos (`RedService.construir` arma el `Grafo`).
- `controller` y `cli` no usan `repository`: piden todo a `service` (p. ej.
  `ConsultaResultadosService` para tablas y figuras).
- `algorithm` y `model` son **puros**: solo `java.lang`, `java.util` (y
  `java.io.Serializable` en el modelo); sin Spring, sin `Constantes`, sin servicios ni datos.
- `service`, `algorithm`, `model` y `repository` no imprimen (`System.out`), salvo `ProcesoAislado` (canal de medición).
- Inyección **por constructor**, sin `@Autowired` en campos. Sin estado mutable compartido
  salvo cachés explícitas (`RedService` guarda el grafo cargado).

```
src/main/java/co/edu/uniquindio/rutas/
  RutasApplication.java, Constantes.java        (Constantes = config del enunciado)
  controller/        LugaresController, ItinerarioController, ResultadosController, ManejoErrores
  cli/               ComandosCli  (maquina, catalogo, r1, r2, r3, r4)
  model/             Grafo, Arista, Punto, Lugar, NodoCercano, ResultadoDijkstra, ResultadoTsp, ...
  algorithm/         Dijkstra, EnvolventeConvexa, TspFuerzaBruta, HeldKarp, VecinoMasCercano,
                      NodoMasCercano, Tsp                                         [PROPIOS]
  service/           RedService, CatalogoService, RutaService, ItinerarioService,
                      ExperimentoR1..R4Service, ConsultaResultadosService, EntornoService,
                       TareasTsp, InstanciaTsp, measurement/ (Medidor, Medicion, Tarea, ProcesoAislado)
  repository/        Csv, RedRepository, CatalogoRepository, LugaresNodosRepository,
                      MatrizRepository, ResultadosRepository, MaquinaRepository, records Fila*
src/main/resources/static/   index.html, app.js, estilos.css (Leaflet)
src/test/java/...            JUnit 5 + ArchUnit (mismo paquete que la clase probada)
python/preparacion/          red, r1_componente, verificaciones, envolvente_scipy, graficas, medicion
specs/ data/ results/ docs/
```

## 4. Reglas inamovibles

1. **Implementación propia** en `algorithm` (y el `Grafo`):
   - Prohibido usar librerías que resuelvan el problema (JGraphT, JTS, Guava graphs, etc.) o
     cualquier dependencia fuera de `java.lang`/`java.util`. Lo verifica ArchUnit.
   - Permitido como estructura de apoyo: `PriorityQueue` (montículo binario), `ArrayList`,
     `ArrayDeque`, `HashMap`, `Arrays.sort`/`List.sort`. Su costo se **investiga y se cita** en
     el documento (Javadoc oficial).
   - La fuerza bruta genera sus propias permutaciones.
   - `ArquitecturaTest` **nunca** se modifica para que pase: se corrige o se mueve el código.
     En una reorganización de paquetes aprobada se actualizan únicamente sus rutas y la
     agrupación de capas equivalente, sin eliminar ni debilitar restricciones.
2. **Spec primero (SDD)**. Antes de escribir código de un requerimiento, lee su spec. Si el
   cambio altera un contrato (firma pública, columnas de un CSV, formato de archivo),
   **actualiza primero la spec** y dilo. No implementes nada que la spec no pida sin preguntar.
3. **Verificar antes de medir** (protocolo 6.6). Un algoritmo rápido pero incorrecto no cuenta.
   Cada `ExperimentoRxService` verifica y aborta si falla; las verificaciones contra librerías
   se hacen en `python/`.
4. **Protocolo experimental** (`specs/protocolo-experimental.md`): medir solo el algoritmo, 1
   calentamiento + 5 repeticiones y mediana, ms con `System.nanoTime`, semilla
   `Constantes.SEMILLA`, límite 5 min con `Medidor.medirConLimite` (JVM aparte). Usa siempre
   `Medidor`; no escribas cronómetros ad hoc. Las mediciones se ejecutan con
   `./gradlew bootRun --args='rN'` (memoria fija `-Xmx2g`), nunca desde el IDE ni las pruebas.
5. **Nada inventado**:
   - Resultados: todo número del documento sale de un CSV de `results/` generado por un
     comando. Nunca escribas ni edites resultados a mano.
   - Fuentes: no inventes citas, páginas, teoremas ni URLs. Si no puedes verificar una fuente,
     escribe `[FUENTE PENDIENTE: qué hay que buscar]` y avísalo.
   - Coordenadas del catálogo: puedes proponer, pero quedan `verificado_mapa = no` hasta que
     una persona las compruebe en un mapa.
   - Complejidades: cada una con su fuente; si es un cálculo propio, mostrar la derivación.
6. **Contratos de datos**: los formatos de `data/processed/` y `results/` los definen los
   repositorios de `repository` (constantes `COLUMNAS*`), `python/preparacion/red.py` y las specs.
   Si cambian en Java, cambian en Python y en la spec en el mismo commit. Las columnas del
   anexo del enunciado se pueden ampliar, nunca reducir.
7. **Claridad antes que astucia**: el código se sustenta en vivo. Nada de streams paralelos,
   trucos de bits sin comentar, `Unsafe`, ni micro-optimizaciones que oculten el algoritmo. Una
   optimización necesaria se discute primero y se documenta su efecto en la complejidad.
8. **Determinismo**: toda aleatoriedad usa `new Random(semilla)` derivada de
   `Constantes.SEMILLA` (en Python, `random.Random`) y la semilla se guarda en el CSV.
9. **Capas** (sección 3): todo código nuevo va en la capa que le corresponde. Si algo "no
   cabe", se pregunta antes de crear un paquete nuevo.

## 5. Estilo de código

- Todos los directorios y paquetes bajo `src/main/java` y `src/test/java` tienen nombres en
  **inglés** (`controller`, `service`, `repository`, `model`, `algorithm`, `cli`,
  `service.measurement`, `architecture` para pruebas). Se conserva el paquete base institucional.
- Identificadores en **español sin tildes** (`nodosExplorados`, `tiempoS`), clases en
  `PascalCase`, constantes en `MAYUSCULAS`. Javadoc y comentarios en español con tildes.
- Unidades en el nombre: `S`, `Ms`, `M`, `Kmh`, `Min` (`tiempoS`, `distanciaM`).
- Resultados y DTO como `record`. Algoritmos como clases `final` con constructor privado y
  métodos `static` que reciben todo por parámetro.
- Javadoc de cada algoritmo: **spec que implementa, idea en 1–3 líneas, complejidad en tiempo y
  memoria, fuente**; en DP y divide y vencerás, también la **recurrencia**.
- Sin `System.out` en `service`/`algorithm`/`model`/`repository`. Excepciones: `UnsupportedOperationException` para lo
  pendiente, `IllegalArgumentException` para entradas inválidas, `IllegalStateException` para
  datos faltantes (la CLI y `ManejoErrores` las muestran bien).
- Pruebas: JUnit 5 + AssertJ o `Assertions`; un método por caso de la spec con su ID en el
  nombre (`t2b_ruta1a6`). Oráculos independientes en las pruebas (Bellman-Ford, cadena
  monótona); librerías externas de verificación solo en `python/`.
- Python (`python/`): PEP 8, type hints, docstrings en español.

## 6. Notación del curso (para el documento técnico y el Javadoc)

Seguir las guías del docente (`docs/guias/`):
- Tiempo de ejecución T(n) con constantes y orden O, Θ, Ω (guías 03–04).
- Recurrencias: inducción/expansión y **Teorema Maestro** en la forma `T(n) = a·T(n/b) + O(n^d)`
  comparando d con log_b a (guías 05 y 07). Si la recurrencia no tiene esa forma (p. ej.
  QuickHull en el peor caso), resolver por expansión y decir que el Teorema Maestro no aplica.
- Memoria de algoritmos recursivos = memoria auxiliar + profundidad de la pila (guía 07, 4.3).
- Voraces: candidatos, selección, factibilidad, solución y objetivo; elección voraz y
  subestructura óptima (guía 08).
- Programación dinámica: subproblemas superpuestos + subestructura óptima; Held-Karp con
  `dp[S][i]` (guía 09).
- Citas autor-año (como las guías). Candidatas en `docs/fuentes.md`, verificadas antes de citar.

## 7. Cómo trabajar una tarea (flujo para el agente)

1. Lee la spec del requerimiento y este archivo. Identifica el ID (p. ej. `R2.1`, `T2-b`).
2. Propón un plan corto (clases, capa, métodos, casos de prueba que se cubren) y espera
   aprobación si el cambio es grande o toca un contrato.
3. Si faltan casos de prueba, agrégalos a la spec y a `src/test/java` **antes** de implementar.
4. Implementa en la clase y la capa que indica la spec, respetando las secciones 3 y 4.
5. Ejecuta `./gradlew test` (o `--tests 'NombreTest'`). Debe pasar `ArquitecturaTest`. No
   declares "listo" con pruebas en rojo; si algo falla y no lo puedes resolver, dilo.
6. Explica al integrante, en 5–10 líneas, la idea del algoritmo, el invariante o la recurrencia
   y la complejidad: debe poder sustentarlo sin ti.
7. Agrega una entrada a `docs/registro-ia.md` (sección 9) y actualiza el **Estado** de la spec.

## 8. Comandos

```bash
# Java (desde la raíz). En Windows: gradlew.bat
./gradlew test                                   # pruebas + ArchUnit
./gradlew test --tests 'co.edu.uniquindio.rutas.algorithm.DijkstraTest'
./gradlew bootRun --args='maquina'               # results/maquina_java.csv
./gradlew bootRun --args='catalogo'              # valida data/catalogo/lugares.csv
./gradlew bootRun --args='r1'                    # también r2, r3, r4
./gradlew bootRun                                # aplicación en http://localhost:8080

# Python (desde python/, con su entorno virtual activo)
pip install -r requirements.txt
python -m pytest tests
python -m preparacion maquina | descargar | procesar | r1
python -m preparacion verificar-lugares | verificar-dijkstra | envolvente-scipy | graficas
```

Orden completo para reproducir: ver `README.md`.

## 9. Git, evidencia y registro de IA

- El historial de commits es **evidencia del trabajo de cada integrante**. El agente **no hace
  commit ni push** salvo que el integrante lo pida en ese momento; nunca reescribe historia
  (`rebase -i`, `push --force`, `reset --hard` sobre trabajo compartido).
- Mensajes de commit en español, estilo convencional con el requerimiento:
  `feat(r2): dijkstra con cola de prioridad`, `test(r4): casos T4-a a T4-d`,
  `docs(r3): recurrencia de quickhull`, `exp(r2): mediciones de 20 pares`.
- Rama por requerimiento (`r2-dijkstra`) y PR hacia `main` revisado por otro integrante.
- **Registro obligatorio** (anexo de uso de IA del enunciado): tras cada tarea significativa,
  agrega al final de `docs/registro-ia.md` una fila con fecha, herramienta y modelo,
  integrante, requerimiento, qué se pidió, qué archivos cambiaron y **cómo se verificó**.

## 10. Decisiones tomadas (resumen; detalle en `docs/decisiones.md`)

| Tema | Decisión | Estado |
|---|---|---|
| Lenguaje | Java 21 para todos los algoritmos; Python solo para datos y verificación | tomada |
| Framework / build | Spring Boot 4.1 + Gradle (wrapper) | tomada |
| Arquitectura | 3 capas con paquetes tradicionales en inglés: `controller`/`cli` → `service` (+ `algorithm`, `model`) → `repository`, verificada con ArchUnit | tomada |
| Representación | Listas de adyacencia (`Grafo`), aristas paralelas → la de menor tiempo | tomada (justificar números en R1.1) |
| Medición | `Medidor`: 1 calentamiento + 5 reps, mediana, ms; JVM hija con límite de 5 min | tomada |
| Semilla | `Constantes.SEMILLA = 2026` | tomada |
| Dijkstra con cola | `PriorityQueue` con borrado perezoso | tomada |
| Experimento R2 | 10 pares intramunicipales + 10 intermunicipales reproducibles; ambas versiones incluyen reconstrucción; matriz Java de segundos a 3 decimales | tomada (2026-11-12, D13) |
| Envolvente | QuickHull (alternativa: por mezcla) | propuesta — confirmar en R3 |
| Heurística R4 | Vecino más cercano | propuesta — confirmar en R4 |
| Velocidades sin `maxspeed` | Promedio por tipo con OSMnx; respaldo promedio entre tipos con datos; límites múltiples por mínimo | tomada (R1.2); detalle en spec |
| `retain_all` en descargas | `True`; `drive`, `simplify=True`, GraphML reutilizable con metadatos; redes R1.3 de 2/5/10/20 km con `dist_type="bbox"` centradas en `cab_armenia` | tomada (departamento 2026-11-10; bbox R1.3 aprobado 2026-11-11, D12) |
| App | REST + página estática con Leaflet | tomada |

## 11. Lo que NO se hace

- Usar JGraphT, JTS u otra librería en `algorithm`, o NetworkX/SciPy para producir
  (no verificar) resultados.
- Escribir un algoritmo propio en Python "porque es más fácil".
- "Arreglar" una prueba o un resultado para que coincida; se corrige el algoritmo o se explica.
- Volver a descargar la red desde Java o desde R2–R5: se lee `data/processed/`.
- Medir incluyendo lectura de archivos, construcción del grafo, conversión de datos o
  impresión; medir desde pruebas JUnit o desde el depurador.
- Saltarse capas: leer CSV desde un controlador o un algoritmo, armar el `Grafo` en `repository`,
  imprimir desde `service`, `algorithm` o `model`.
- Agregar dependencias a `build.gradle` o `python/requirements.txt` sin explicar para qué.
