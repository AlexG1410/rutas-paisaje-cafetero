# R2 — Caminos mínimos

Estado: implementado, verificado y experimentado; análisis y figura disponibles (2026-11-12).
Mejora experimental: figura logarítmica, terminología del enunciado y normalización
T/(k·V) generada desde CSV, con discusión de inicialización y límites de inferencia.
Responsable: _(asignar)_ · Peso: 20 %.

Evidencia actual: 20 pares con cola, 10 también con lineal, 30 mediciones completadas
con cinco repeticiones. Diez comparaciones Java entre versiones, treinta resultados
contra NetworkX y matriz 40×40 (1600 celdas) sin diferencias. Datos de máquina, tablas y
figura generados por comandos; análisis en `docs/documento-tecnico/03-r2-caminos-minimos.md`.
La suite Java global conserva fallos de R3/R4 todavía no implementados; no se ocultan
ni se presentan como fallos del bloque R2. Evidencia de pruebas en `docs/registro-ia.md`.

## Objetivo

Implementar Dijkstra (propio) en dos versiones sobre la componente de R1 con peso
`tiempo_s`, medirlas, compararlas y construir la matriz de tiempos entre lugares.

## Contrato

```java
// algorithm.Dijkstra (PROPIO, métodos static)
int[] reconstruirRuta(int[] predecesor, int origen, int destino)
ResultadoDijkstra lineal(Grafo grafo, int origen, int destino)
ResultadoDijkstra colaPrioridad(Grafo grafo, int origen, int destino)  // destino = SIN_DESTINO (-1): todas
// model.ResultadoDijkstra(double tiempoS, int[] ruta, int nodosExplorados, double[] distancias)
```

Capas: los algoritmos en `algorithm.Dijkstra` (puros); el experimento en
`service.ExperimentoR2Service` (`./gradlew bootRun --args='r2'`), que lee y escribe
con los repositorios de `repository` (`MatrizRepository`, `ResultadosRepository`).

Archivos: `results/r2/pares.csv`, `mediciones_dijkstra.csv`, `cociente.csv`,
`verificacion.csv`, `matriz_tiempos.csv`; luego Python escribe `verificacion_networkx.csv`.

## Comportamiento esperado

1. `dist[origen] = 0`, `dist[v] = ∞` para los demás; `predecesor[v] = -1`.
2. **Versión lineal, O(V²)**: en cada iteración se recorre el arreglo `dist` completo para
   elegir el nodo no fijado de menor `dist` (finito). Sin cola de prioridad.
3. **Versión con cola, O((V + E) log V)**: `java.util.PriorityQueue` con entradas
   `(dist, nodo)` (p. ej. un `record` que implemente `Comparable`, desempate por nodo) y
   **borrado perezoso**: al relajar se inserta una entrada nueva; al sacar una entrada de un nodo ya
   fijado, se descarta sin contarla.
4. **Nodos explorados**: un nodo cuenta una sola vez, la primera vez que sale de la cola (o
   que la versión lineal lo elige). El destino cuenta.
5. **Parada temprana**: ambas se detienen en cuanto fijan el `destino`.
6. Relajación: si `dist[u] + w(u, v) < dist[v]`, actualizar `dist[v]` y `predecesor[v] = u`.
7. `ruta` se reconstruye con `predecesor` desde el destino hasta el origen y se invierte.
   Destino inalcanzable → `tiempoS = Double.POSITIVE_INFINITY`, `ruta = {}`. Origen =
   destino → `tiempoS = 0`, `ruta = {origen}`, `nodosExplorados = 1`.
8. `dijkstra_cola(..., destino=None)` no se detiene y devuelve `distancias` completas
   (se usa para la matriz).
9. **Matriz de tiempos**: para cada lugar i del catálogo (≥ 40, incluida Armenia), una sola
   ejecución `dijkstra_cola(nodo_i, None)`; `M[i][j] = distancias[nodo_j]`. Diagonal 0. No es
   simétrica. Formato: `MatrizRepository.guardar` (ids en primera fila y columna,
    segundos).

### Precisiones de entrada y resultado

- Grafo no nulo, origen y destino en `0..V-1`; `SIN_DESTINO=-1` admitido en ambas
  versiones para obtener todas las distancias. Entradas inválidas: `IllegalArgumentException`.
- Desempate de selección por menor índice en ambas versiones. La relajación es estricta;
  pesos cero se admiten, aristas negativas/no finitas se rechazan al construir `Grafo`.
  Las entradas obsoletas de cola no cuentan como nodos explorados.
- Sin destino: tiempo `+∞`, ruta vacía y distancias de todos los nodos alcanzables.
  Con parada temprana, solo las distancias de nodos fijados son definitivas; las otras
  pueden ser tentativas. No usarlas para la matriz.
- `reconstruirRuta` rechaza índices inválidos o predecesores cíclicos/fuera de rango;
  una cadena que termina en -1 sin llegar al origen devuelve ruta vacía. Origen igual
  destino devuelve `[origen]`. Usa memoria O(longitud de ruta), sin arreglos V adicionales.
- Suma de pesos finitos que desborde `double`: error explícito, no confundir con ausencia
  de camino. La garantía de finitud en la red real se verifica al construir la matriz.

### Experimento aprobado y contratos de archivos

- Ordenar lugares por ID; generar pares dirigidos con IDs y nodos distintos. Separar
  candidatos del mismo municipio y de municipios diferentes, barajar cada lista con
  `Random(Constantes.SEMILLA)` y elegir 10 de cada una. No se afirma que todos los
  pares del mismo municipio tengan poca distancia vial. Con candidatos insuficientes,
  abortar sin escoger pares repetidos ni cambiar silenciosamente el criterio.
- Medir los 20 pares con cola y los 10 del mismo municipio también con lineal. Incluye
  reconstrucción de ruta en ambas. Un proceso JVM por serie con `Medidor.medirConLimite`;
  preparar/cargar/serializar el grafo queda fuera del cronómetro. `service.InstanciaDijkstra`
  lleva grafo/origen/destino y `service.TareasDijkstra` solo invoca algoritmos. `Grafo`,
  `Arista` y `ResultadoDijkstra` admiten `java.io.Serializable` para este transporte local.
- Primero validar catálogo y asociaciones (IDs exactos, únicos, nodos en rango), construir
  matriz finita y diagonal cero, comprobar costo/extremos/aristas de las rutas de los
  20 pares y contrastar ambas versiones en los 10 comunes (1e-6 s absoluto). Después
  medir y validar de nuevo cada resultado terminado. Errores abortan antes de publicar
  tablas; timeout es un estado experimental, no un resultado correcto ficticio.
- `pares.csv`: `origen,destino,nodo_origen,nodo_destino,municipio_origen,municipio_destino,
  criterio,medir_lineal,semilla`; criterio `mismo_municipio` o `intermunicipal`.
- `verificacion.csv`: `origen,destino,tiempo_lineal_s,tiempo_cola_s,coinciden`, 10 filas.
- `mediciones_dijkstra.csv`: columnas base de la sección Experimento más `ruta_nodos`
  (índices separados por `|`); 30 filas. Si no completada: tiempo, costo, explorados y
  ruta vacíos; no fabricar resultados parciales.
- `cociente.csv`: `origen,destino,cociente,estado`, 10 cocientes por par más filas
  `mediana`, `minimo`, `maximo` en `origen` (destino vacío). Resúmenes solo si los 10
  cocientes son válidos: ambas series completadas y mediana cola >0; de lo contrario
  resumen vacío/estado `no disponible`. No dividir por cero ni omitir faltantes.
- `matriz_tiempos.csv` conserva el contrato existente: `id,<ids...>` y segundos con
  tres decimales. La comparación externa de esta matriz admite 0.0005001 s por redondeo;
  pares medidos conservan precisión de `double` y tolerancia 1e-6 s.
- `ResultadosRepository.COLUMNAS_*` fija cabeceras R2; no cambia columnas de R1.
  El comando `maquina` registra el entorno Java antes de medir con `bootRun -Xmx2g`.
- `verificar-dijkstra` no descarga: lee aristas dirigidas procesadas, conserva el mínimo
  tiempo por par para NetworkX, contrasta todos los resultados completados (incluidos
  costos de rutas) y todas las celdas de la matriz. Publica
  `verificacion_networkx.csv` con `origen,destino,tiempo_java_s,tiempo_networkx_s,coinciden`
  (una fila por medición completada; no completadas no tienen resultado que contrastar)
  y `verificacion_matriz_networkx.csv`: `origen,destino,tiempo_java_s,tiempo_networkx_s,
  coinciden` (L² filas). Aborta por discrepancias, IDs duplicados/faltantes, índices
  inválidos o archivos inconsistentes. La verificación no produce la matriz usada por R4.
- Figura R2: `results/figuras/r2_tiempo_vs_explorados.png` desde mediciones CSV,
  ambas versiones en una figura, eje x «Nodos explorados (cantidad)», eje y tiempo en
  ms con escala logarítmica. Explorados significa nodos fijados por primera vez, no
  entradas obsoletas extraídas. Para esta figura se exigen tiempos completados >0;
  cero no es representable en log y se rechaza sin sustituirlo por épsilon. Timeouts
  se anuncian sin asignarles un tiempo ni cantidad de nodos ficticios.
  Si todas son timeout, mostrar «Sin mediciones completadas» sin marcas numéricas
  verticales; la ventana positiva necesaria para dibujar en log no representa datos.
- Posprocesamiento aprobado (2026-11-12), sin nuevas mediciones: `python -m preparacion
  graficas` también genera `results/r2/normalizacion_lineal.csv`, una fila por medición
  lineal, columnas `origen,destino,V,nodos_explorados,tiempo_ms,ns_por_posicion,estado`.
  `ns_por_posicion = tiempo_ms * 1e6 / (nodos_explorados * V)`. V se obtiene de las
  filas `grafo_java` de `results/r1/memoria_representacion.csv`: deben existir y declarar
  un mismo entero positivo. Exigir k entero en 1..V en las mediciones completadas.
  En timeout se conservan IDs/V/estado y quedan vacíos k, tiempo y cociente. Validar
  antes de sobrescribir las salidas R2; no modificar las mediciones de entrada. Este
  CSV es análisis derivado, no un algoritmo propio ni una medición aislada del barrido.
  No filtrar k<200 en el CSV: el umbral solo sirve para discutir los datos observados.

## Restricciones

- Implementación propia: solo `PriorityQueue` como estructura de apoyo (investigar y citar
  el costo de `offer`/`poll`: O(log n) según su Javadoc oficial).
- NetworkX solo en la verificación de `python/` (`verificar-dijkstra`); en las pruebas Java el
  oráculo es Bellman-Ford.

## Casos de prueba

Grafo de la guía 08 del curso (Algoritmos voraces, ejemplo de Kruskal/Prim/Dijkstra), no
dirigido, nodos 1..7 (en código 0..6): {1,2}=1, {2,3}=2, {4,5}=3, {6,7}=3, {1,4}=4, {2,5}=4,
{4,7}=4, {3,5}=5, {2,4}=6, {3,6}=6, {5,7}=7, {5,6}=8.

| ID | Entrada | Salida esperada | Origen |
|---|---|---|---|
| T2-a | grafo guía, origen 1, sin destino | dist: 2→1, 3→3, 4→4, 5→5, 6→9, 7→8 | tabla paso a paso de la guía 08 |
| T2-b | grafo guía, 1 → 6 (ambas versiones) | tiempo 9, ruta 1-2-3-6 | a mano (1+2+6; 1-4-7-6 = 11) |
| T2-c | grafo guía, 1 → 7 (ambas versiones) | tiempo 8, ruta 1-4-7 | a mano (1-2-5-7 = 12) |
| T2-d | dirigido 0→1 (1), 1→0 (5) | d(0,1)=1, d(1,0)=5 | asimetría |
| T2-e | 2 nodos sin aristas, 0 → 1 | ∞, ruta [], explorados 1 | inalcanzable |
| T2-f | origen = destino | 0, [origen], explorados 1 | borde |
| T2-g | cadena 0→1→2→3→4 (peso 1), 0 → 1 | explorados = 2 | parada temprana |
| T2-h | 30 grafos dirigidos al azar (semilla) | ambas versiones = Bellman-Ford (oráculo en la prueba); la ruta tiene ese costo | verificación |
| T2-i | ciclo con pesos cero, empates y entradas obsoletas | rutas válidas y cada nodo cuenta una vez | manual |
| T2-j | grafo/origen/destino o predecesores inválidos | error claro; cadena sin origen devuelve vacío | validación |
| T2-k | dos pesos cuya suma desborda double | error explícito | borde numérico |
| T2.I-a | catálogo sintético y red asimétrica; medidor sustituido | 20 pares reproducibles, 30 llamadas, matriz/columnas/cocientes correctos | integración |
| T2.I-b | IDs/nodos inválidos o grafo no conexo | no mide ni sobrescribe resultados | integración |
| T2.I-c | timeout o resultado medido incorrecto | timeout sin cifras ficticias; error no publica tablas | integración |
| T2.V-a | CSV Java sintético correcto (paralelas y asimetría) | verificación de pares/rutas y matriz sin HTTP | NetworkX |
| T2.V-b | costo/ruta/matriz/IDs incorrectos | error o discrepancia explícita, sin corregir Java | verificación |
| T2.F-a | resultados completados y timeout | figura con dos versiones y timeout indicado sin datos ficticios | Matplotlib |
| T2.F-b | tiempos positivos, cero y timeout | escala logarítmica y etiqueta explorados; cero se rechaza | Matplotlib |
| T2.N-a | V=100, k=30, T=4 ms y timeout | 1333.333… ns por posición; timeout sin cifras; entradas intactas | cálculo manual |
| T2.N-b | V ausente/no entero/no finito/inconsistente, k>V | error antes de publicar salidas R2 | validación |

## Criterios de aceptación

- [x] T2-* en verde.
- [x] Verificación en la red real: en los 10 pares medidos con ambas versiones, los tiempos
      coinciden (tolerancia absoluta 1e-6 s) → `results/r2/verificacion.csv`.
- [x] ≥ 20 pares con la versión con cola; 10 de ellos (lugares cercanos: mismo municipio o
      municipios vecinos) también con la lineal. Pares y criterio en `pares.csv`.
- [x] Figura nodos explorados vs tiempo (ms), ambas versiones en la misma figura.
- [x] Cociente lineal/cola en los 10 pares: mediana, mínimo, máximo → `cociente.csv`.
- [x] Matriz ≥ 40 × 40 guardada; sin infinitos (todo está en la SCC).
- [x] Verificación externa de rutas/costos y todas las celdas de matriz contra NetworkX.
- [x] Documento: invariante, elementos voraces, pesos negativos, tiempo y memoria derivados,
      fuentes verificadas, tablas desde CSV y discusión de límites del experimento.

## Análisis teórico (va al documento técnico)

- Complejidad en tiempo y memoria de ambas versiones, con fuente (p. ej. Cormen et al.,
  *Introduction to Algorithms*, capítulo de caminos mínimos desde una fuente; verificar el
  número de capítulo en la edición citada). Con borrado perezoso la cola puede tener hasta
  E entradas: O((V + E) log E) = O((V + E) log V) porque E ≤ V².
- **Invariante** (enunciarlo en el documento): cuando un nodo u se fija, `dist[u]` es la
  distancia mínima real δ(origen, u); además, para todo nodo no fijado, `dist[v]` es el
  menor costo de un camino a v cuyos nodos intermedios están todos fijados.
- **Por qué se puede parar al fijar el destino**: por el invariante, su distancia ya es
  definitiva y ningún camino posterior puede mejorarla.
- **Condición sobre los pesos**: no negativos. Con un peso negativo el invariante falla
  (mostrar un contraejemplo de 3 nodos); para ese caso existe Bellman-Ford (guía 09).
- Relación con la guía 08: elementos voraces de Dijkstra (candidatos, selección,
  factibilidad, solución, objetivo).

## Experimento

`mediciones_dijkstra.csv`: `origen, destino, version, tiempo_viaje_s, nodos_explorados,
tiempo_ms, repeticiones, estado` (una fila por consulta y por versión; `version` ∈
{`lineal`, `cola_prioridad`}). La versión lineal en la red completa puede tardar mucho:
el protocolo (una sola medición si pasa de 1 min) lo cubre.

## Decisiones abiertas

Resueltas por aprobación del bloque (2026-11-12): 10 pares del mismo municipio y 10
intermunicipales, selección reproducible; ambas versiones incluyen reconstrucción de ruta.
