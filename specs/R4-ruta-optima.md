# R4 — Ruta óptima de recorrido (viajante asimétrico)

Estado: borrador · Responsable: _(asignar)_ · Peso: 25 % · Paquetes reorganizados: 2026-11-10 (sin cambio funcional)

## Objetivo

Salir de un lugar, visitar k lugares y regresar con el menor tiempo total según la matriz de
R2. Comparar fuerza bruta, Held-Karp y una heurística voraz; armar el itinerario de un día.

## Contrato

```java
// algorithm.Tsp (ya implementado)
static double costoRecorrido(double[][] matriz, int[] recorrido)
static boolean esRecorridoValido(int[] recorrido, int[] lugares)
// algorithm (PROPIOS)
TspFuerzaBruta.resolver(double[][] matriz, int[] lugares) -> ResultadoTsp
HeldKarp.resolver(double[][] matriz, int[] lugares)       -> ResultadoTsp
HeldKarp.entradasTabla(int k) -> long                       // k · 2^k
VecinoMasCercano.resolver(double[][] matriz, int[] lugares) -> ResultadoTsp
// model.ResultadoTsp(double costoS, int[] recorrido)
// service.TareasTsp.{FuerzaBruta, HeldKarpTarea, Heuristica}: para Medidor.medirConLimite
```

Experimento: `service.ExperimentoR4Service` (`./gradlew bootRun --args='r4'`);
itinerario: `service.ItinerarioService` (también lo usa la app).

`matriz[i][j]` = segundos de i a j (no simétrica). `lugares[0]` = partida; `lugares[1..k]`
= los k lugares. `recorrido` = `{partida, ..., partida}`, longitud k + 2, cada lugar una vez.

## Comportamiento esperado

### R4.1 Soluciones exactas
1. **Fuerza bruta**: generador **propio** de permutaciones de `lugares[1..k]` (p. ej.
   algoritmo de Heap o recursión con intercambios). Evalúa todas; en empate conserva la
   primera encontrada.
2. **Held-Karp** (notación de la guía 09, con S sobre los k lugares y la partida 0 implícita):
   - `dp[{j}][j] = M[0][j]`
   - `dp[S][j] = min_{i ∈ S − {j}} ( dp[S − {j}][i] + M[i][j] )` para |S| ≥ 2, j ∈ S
   - `OPT = min_{j} ( dp[U][j] + M[j][0] )`, U = todos los k lugares
   - S como máscara de bits; tabla de **predecesores** para reconstruir el orden de visita.
3. Antes de implementar, justificar en el documento la **subestructura óptima**: si el
   recorrido óptimo que visita S y termina en j pasa por i justo antes de j, entonces su
   prefijo es óptimo para (S − {j}, i); si no, se reemplaza por uno mejor y se contradice
   la optimalidad.
4. k = 0 → `{p, p}`, costo 0. k = 1 → `{p, a, p}`.

### R4.2 Heurística voraz
Recomendación inicial: **vecino más cercano** (desde el lugar actual, ir al no visitado con
menor `M[actual][j]`; empate → el que aparece primero en `lugares`; al final regresar).
Justificar la elección en el documento con los elementos voraces de la guía 08.

### R4.3 Calidad
`brecha_% = 100 · (costo_heurística − costo_HK) / costo_HK`, para k = 5, 8, 10, 12, con ≥ 5
instancias por k. Reportar promedio y máximo.

### R4.4 Itinerario de un día
1. Partida (por defecto `cab_armenia`) y entre 6 y 8 atractivos elegidos por el equipo.
2. Ordenar con Held-Karp. Duración = Σ desplazamientos (matriz, en min) + Σ visitas
   (`duracion_visita_min` del catálogo). La partida no tiene visita.
3. Si supera 11 h (660 min): retirar atractivos con un **criterio justificado**, recalcular
   con Held-Karp y reportar la duración final. Criterio sugerido: en cada paso retirar el
   atractivo cuya eliminación más reduce la duración total (probar cada uno es barato con
   k ≤ 8); alternativa: prioridad por interés definida en el catálogo.
4. Tabla del anexo: orden, lugar, desplazamiento desde el anterior (min), visita (min),
   tiempo acumulado (h), fila de regreso; al final totales de visita, desplazamiento y jornada.

### R4.5 Voraz frente a óptimo
Ejemplo donde el vecino más cercano no es óptimo (T4-b y, si aparece, uno con datos
reales). Explicar por qué en Dijkstra la elección voraz sí garantiza el óptimo (propiedad de
elección voraz + subestructura óptima, guía 08 sección 6) y aquí no.

## Restricciones

- Las tres son implementación propia (`java.lang`/`java.util`, verificado por ArchUnit).
- Instancias de experimento: partida Armenia; k lugares al azar entre **todas** las
  categorías del catálogo (sin Armenia), con semilla derivada de `Constantes.SEMILLA` y guardadas
  en `results/r4/instancias.csv`.

## Casos de prueba

| ID | Entrada | Salida esperada | Origen |
|---|---|---|---|
| T4-a | matriz A,B,C,D de la guía 09: `[[0,10,15,20],[10,0,35,25],[15,35,0,30],[20,25,30,0]]`, lugares [0,1,2,3] | costo 80 (fuerza bruta y Held-Karp); recorrido válido de costo 80 | tabla de la guía 09 |
| T4-b | `[[0,7,8,6],[7,0,9,2],[8,9,0,1],[6,2,1,0]]`, lugares [0,1,2,3] | vecino más cercano: 23, `[0,3,2,1,0]`; óptimo: 18 | a mano (A→D→C→B→A vs A→B→D→C→A) |
| T4-c | asimétrica `[[0,1,9],[9,0,1],[1,9,0]]`, lugares [0,1,2] | 3, `[0,1,2,0]` (el sentido inverso cuesta 27) | a mano |
| T4-d | lugares [0] y [0,1] | `[0,0]` costo 0; `[0,1,0]` costo M01+M10 | borde |
| T4-e | lugares no consecutivos de una matriz 8×8: [5,2,7,0] | fuerza bruta = Held-Karp | indexación |
| T4-f | 40 matrices asimétricas al azar, k = 1..7 (semilla) | fuerza bruta = Held-Karp; recorridos válidos; `Tsp.costoRecorrido == costoS` | verificación cruzada |
| T4-g | `HeldKarp.entradasTabla(k)` | `k · 2^k` | definición |
| T4-h | heurística en T4-f | recorrido válido y costo ≥ óptimo | propiedad |

## Criterios de aceptación

- [ ] T4-* en verde (`TspTest`).
- [ ] Fuerza bruta y Held-Karp dan el mismo costo óptimo en todos los k que la fuerza bruta
      resuelve (columna `coinciden`).
- [ ] Tabla y figura tiempo vs k (5..12) para ambos; mayor k resuelto por cada uno;
      "no completada" si pasa de 5 min (`Medidor.medirConLimite`).
- [ ] Memoria de Held-Karp: entradas de la tabla y bytes estimados por k (y opcionalmente
      la memoria medida de la JVM, en una ejecución aparte de la de tiempo).
- [ ] Tabla de calidad de la heurística; itinerario con totales; contraejemplo voraz.

## Análisis teórico (va al documento técnico)

- Fuerza bruta: Θ(k!) recorridos, Θ(k·k!) operaciones si cada costo se calcula de nuevo;
  memoria O(k).
- Held-Karp: Θ(k² · 2^k) tiempo, Θ(k · 2^k) memoria (dp + predecesores); recurrencia de
  arriba; fuente: Held y Karp (1962) y la guía 09.
- Vecino más cercano: Θ(k²) tiempo, O(k) memoria; sin garantía de optimalidad.
- Comparar con lo medido: el cociente t(k+1)/t(k) debería acercarse a (k+1) en fuerza bruta
  y a ≈ 2·((k+1)/k)² en Held-Karp.

## Experimento

- `exactas.csv`: `k, tiempo_fuerza_bruta_ms, estado_fuerza_bruta, tiempo_held_karp_ms,
  estado_held_karp, costo_fuerza_bruta_s, costo_held_karp_s, coinciden`. Al primer k en que
  un método no completa, no se prueban k mayores para ese método (se marcan "no completada").
- `calidad_heuristica.csv`: `k, instancias, brecha_promedio_pct, brecha_maxima_pct`.
- Figura `r4_tiempo_vs_k.png`, eje y en escala logarítmica.

## Decisiones abiertas

1. Heurística final (vecino más cercano u otra voraz) y su justificación.
2. Atractivos del itinerario y criterio para retirar atractivos.
3. Política de empates (documentada arriba; cambiarla exige actualizar T4-b).
