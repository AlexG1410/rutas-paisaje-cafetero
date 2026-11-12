# R3 — Envolvente convexa de la red vial

Estado: borrador · Responsable: _(asignar)_ · Peso: 20 % · Paquetes reorganizados: 2026-11-10 (sin cambio funcional)

## Objetivo

Calcular la envolvente convexa de los nodos de la componente (como puntos del plano) con un
algoritmo propio de **divide y vencerás**, verificarla contra `scipy.spatial.ConvexHull` y
comparar tiempos con puntos de la red y puntos al azar.

## Contrato

```java
// service.ExperimentoR3Service (redondearYDeduplicar ya implementado)
static List<Punto> redondearYDeduplicar(double[] xs, double[] ys)
// algorithm.EnvolventeConvexa (PROPIO)
static long orientacion(Punto p, Punto q, Punto r)
static List<Punto> calcular(List<Punto> puntos)
// model.Punto(long x, long y), orden natural por (x, y)
```

Flujo: Java (`./gradlew bootRun --args='r3'`) prepara los conjuntos, mide su algoritmo y
exporta puntos y vértices; Python (`python -m preparacion envolvente-scipy`) verifica contra
SciPy sobre los MISMOS puntos, mide SciPy y arma la tabla final.

Archivos: `results/r3/puntos_<conjunto>.csv`, `vertices_<conjunto>.csv`,
`envolvente_propio.csv` (Java), `envolvente.csv` (Python, tabla final),
`envolvente_red.csv` (vértices con lat/lon, para R5).

## Comportamiento esperado

1. Preparación (fuera de la medición): UTM 18N → entero más cercano
   (`Math.floor(v + 0.5)`) → sin repetidos (`redondearYDeduplicar`, conserva el orden de
   aparición y NO ordena). Coordenadas `long` de 64 bits, como pide el enunciado: productos
   cruz del orden de 10^12, sin desbordamiento.
2. `orientacion(p, q, r) = (qx − px)(ry − py) − (qy − py)(rx − px)` en enteros exactos.
3. Salida: vértices en **sentido antihorario**, empezando por el punto con menor `(x, y)`
   (menor x; en empate, menor y). Sin repetir el primero al final.
4. **Colineales sobre un lado no son vértices**: solo se aceptan giros estrictos.
5. Casos degenerados: 0 puntos → `[]`; 1 punto → `[p]`; todos colineales (incluye 2 puntos)
   → los dos extremos `[min, max]` según (x, y).
6. El algoritmo no debe suponer que la entrada viene ordenada; si necesita ordenar, ordena
   dentro de la función (y ese costo entra en la medición y en el análisis).

## Restricciones

- Implementación propia, divide y vencerás, solo `java.lang`/`java.util`. Se permite
  `List.sort`/`Collections.sort` (TimSort según su Javadoc; citar su costo).
- SciPy solo en `python/` para verificar y para la comparación de tiempos.

## Casos de prueba

| ID | Entrada | Salida esperada | Origen |
|---|---|---|---|
| T3-a | `orientacion((0,0),(1,0),(0,1))`, la inversa, y `(0,0),(1,1),(2,2)` | `> 0`, `< 0`, `0` | a mano |
| T3-b | (0,0),(4,0),(4,4),(0,4),(2,0),(4,2),(2,2),(1,3) | `[(0,0),(4,0),(4,4),(0,4)]` | a mano: (2,0) y (4,2) colineales |
| T3-c | (0,0),(1,1),(2,2),(3,3) desordenados | `[(0,0),(3,3)]` | degenerado |
| T3-d | `[]`, `[(1,2)]` | `[]`, `[(1,2)]` | borde |
| T3-e | triángulo (0,0),(10,0),(5,8) + 20 puntos interiores | los 3 vértices | a mano |
| T3-f | 2 000 puntos al azar en un cuadrado (semilla) | mismos vértices que la cadena monótona de Andrew (oráculo de la prueba), como conjunto; antihorario | verificación |
| T3-g | ~500 puntos enteros cerca de una circunferencia | mismos vértices que el oráculo | muchos vértices |
| T3-h | `redondearYDeduplicar({0.5, 1.49, 1.5}, {2.5, 0, 0})` | `[(1, 3), (1, 0), (2, 0)]` | redondeo .5 hacia arriba |

## Criterios de aceptación

- [ ] T3-* en verde (`EnvolventeConvexaTest`, `CatalogoServiceTest.t3h_...`).
- [ ] Verificación contra SciPy en **todas** las pruebas del experimento (columna
      `coinciden` = True en `envolvente.csv`).
- [ ] Tabla con los 4 conjuntos: nodos de la red; cuadrado con 1 000, 10 000 y 50 000 puntos.

## Análisis teórico (va al documento técnico)

- Algoritmo elegido, **justificado según la distribución de los puntos de la red**: los
  nodos se concentran en cascos urbanos y a lo largo de vías; casi todos son interiores y la
  envolvente tiene pocos vértices.
- Recurrencia y su solución (Teorema Maestro cuando aplique, con a, b, d como en la guía 07;
  expansión cuando no tiene la forma a·T(n/b)). Complejidad en tiempo (mejor/promedio/peor)
  y memoria (incluida la pila de recursión, guía 07 sección 4.3), con fuente.
- Qué algoritmo y qué lenguaje usa SciPy internamente (documentación de
  `scipy.spatial.ConvexHull` → biblioteca Qhull; artículo de Barber, Dobkin y Huhdanpaa,
  1996). Explicar la diferencia de tiempos: algoritmo, C compilado frente a Java con JIT, y el
  costo de crear objetos `Punto` frente a un arreglo contiguo de `double`.

## Experimento

`envolvente.csv`: `conjunto, n, vertices, tiempo_propio_ms, tiempo_scipy_ms, cociente,
coinciden, repeticiones, semilla`. `n` es el número de puntos **después** de quitar
repetidos. Puntos al azar: `new Random(Constantes.SEMILLA)` en Java, coordenadas enteras en
`[0, LADO_CUADRADO)`, exportados para que SciPy use los mismos. Medir SciPy pasando un
`numpy.ndarray` ya construido (la conversión queda fuera de la medición), con la misma
política de calentamiento y repeticiones que Java. `cociente = tiempo_propio_ms / tiempo_scipy_ms`.

## Decisiones abiertas

1. **Algoritmo**: recomendación inicial **QuickHull** (divide y vencerás; descarta pronto
   los puntos interiores, que en la red son la gran mayoría; es la misma idea que usa Qhull,
   lo que hace interesante la comparación). Alternativa con peor caso garantizado
   O(n log n): envolvente por mezcla (Preparata y Hong, 1977), T(n) = 2T(n/2) + O(n).
   Si se elige QuickHull, cuidar la profundidad de recursión en el peor caso (`bootRun` usa
   `-Xss16m`).
2. Si SciPy y el algoritmo propio difieren en puntos colineales, investigar las opciones de
   Qhull y documentarlo (no "arreglar" el resultado propio para que coincida).
