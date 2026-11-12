# R2 — Caminos mínimos

Estado: borrador · Responsable: _(asignar)_ · Peso: 20 % · Paquetes reorganizados: 2026-11-10 (sin cambio funcional)

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

## Criterios de aceptación

- [ ] T2-* en verde.
- [ ] Verificación en la red real: en los 10 pares medidos con ambas versiones, los tiempos
      coinciden (tolerancia absoluta 1e-6 s) → `results/r2/verificacion.csv`.
- [ ] ≥ 20 pares con la versión con cola; 10 de ellos (lugares cercanos: mismo municipio o
      municipios vecinos) también con la lineal. Pares y criterio en `pares.csv`.
- [ ] Figura nodos explorados vs tiempo (ms), ambas versiones en la misma figura.
- [ ] Cociente lineal/cola en los 10 pares: mediana, mínimo, máximo → `cociente.csv`.
- [ ] Matriz ≥ 40 × 40 guardada; sin infinitos (todo está en la SCC).

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

1. Criterio para elegir los 20 pares (y cuáles son "municipios vecinos").
2. ¿La medición de los 20 pares incluye la reconstrucción de la ruta? (Recomendado: sí,
   las dos versiones la hacen y es parte del resultado pedido.)
