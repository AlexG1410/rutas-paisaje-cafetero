# R3 — Envolvente convexa de la red vial

Estado: implementado, verificado y experimentado; documento y figura disponibles (2026-11-12).
Responsable: _(asignar)_ · Peso: 20 %.

Mejora documental aprobada (2026-11-12): discusión más directa, normalización por
punto y revisión del promedio. `graficas` genera `results/r3/tiempo_por_punto.csv`
desde `envolvente.csv`, sin repetir mediciones: columnas `conjunto,n,
tiempo_propio_ms,tiempo_scipy_ms,propio_us_por_punto,scipy_us_por_punto,
estado_propio,estado_scipy`. Se calcula `1000·tiempo_ms/n`; n debe ser entero positivo
y cada timeout conserva su tiempo y normalización vacíos. Casos T3.F-b: fórmula,
tamaño inválido y timeout. Los CSV medidos mantienen su contrato y contenido.
La incorporación de Rényi–Sulanke requiere verificar el artículo original y distinguir
el modelo uniforme continuo del generador entero; hasta entonces, fuente pendiente.
Verificación de esta mejora: Python 171/171 y Java focalizado en EnvolventeConvexa,
ExperimentoR3Service y arquitectura aprobado. Normalización generada con `graficas`;
mediciones y algoritmos originales conservados. Revisión bibliográfica original pendiente.

Evidencia: cuatro conjuntos coinciden con SciPy antes y después de medir; cinco
repeticiones por método sin timeout. Red: 8791 puntos y 14 vértices. Tabla final,
conjuntos, vértices, datos de máquina y figura generados por comandos en results/r3/
y results/figuras/r3_tiempos.png. Python 166/166; Java completo 86/93 aprobadas,
con 7 fallos de TspTest por R4 pendiente; R1–R3 y arquitectura pasan. No se declara
el proyecto completo ni se deshabilitan pruebas pendientes.

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

### Precisiones aprobadas para el bloque completo

- QuickHull 2D propio, sin ordenamiento: extremos lexicográficos; pivote más alejado
  por producto cruz, empate por menor (x,y); particiones estrictamente a la derecha
  de cada segmento. Recorrer la cadena inferior y luego superior produce CCW.
  Trabajar en rangos disjuntos de una copia de referencias a puntos, sin modificar la
  entrada ni mantener listas de candidatos nuevas en cada nivel. Esto permite O(n)
  de arreglo auxiliar más O(h) de salida y profundidad O(n) en el peor caso.
- Lista/puntos null: IllegalArgumentException. Se toleran duplicados en calcular;
  no se cuentan como vértices extra. orientacion usa operaciones long exactas con
  Math.*Exact; si una resta/producto/cruz desborda, IllegalArgumentException, nunca
  un giro silenciosamente incorrecto. No se promete resolver todos los long posibles.
- redondearYDeduplicar rechaza arreglos null/de distinto tamaño, NaN/infinito y valores
  redondeados fuera de long. El experimento exige rango por eje <= 1_000_000_000 m,
  suficiente para que diferencias y cruces de estos conjuntos quepan en long.
- Preparar cuatro conjuntos: red; cuadrado_1000, cuadrado_10000, cuadrado_50000. Cada
  cuadrado genera exactamente el número nominal de muestras, con una instancia nueva
  Random(SEMILLA) por conjunto y nextLong(LADO_CUADRADO) por coordenada. Eliminar
  repetidos conservando orden; n registra el tamaño resultante, no se repone una muestra.
- Antes de medir todos los conjuntos, validar salida: vértices únicos pertenecientes
  a la entrada, mínimo primero, giros estrictos CCW y todos los puntos en el lado
  interior de cada arista. Degenerados se validan aparte. Verificación geométrica
  fuera del reloj; después de cada serie completada exigir la misma lista de vértices.
- Medir con Medidor.medirConLimite y TareaEnvolvente, pasando ArrayList<Punto> ya
  preparada; Punto implementa Serializable para transporte local. QuickHull devuelve
  una ArrayList serializable (firma pública List), sin copiar resultado en el adaptador.
  Solo se mide calcular, incluida su copia auxiliar, no lectura/preparación/validación.
- COLUMNAS_PUNTOS_R3 = x,y (tanto puntos como vértices en orden canónico).
  COLUMNAS_ENVOLVENTE_PROPIO_R3 = conjunto,n,vertices,tiempo_propio_ms,repeticiones,
  semilla,estado. Timeout deja tiempo vacío; vértices exportados son la salida verificada
  previa (no una falsa salida de la medición). No sobrescribir salidas por error previo
  o resultado medido incorrecto. ResultadosRepository fija estas cabeceras.
- envolvente_red.csv: orden,x,y,idx_nodo,lat,lon. Para cada vértice UTM redondeado se
  conserva el primer nodo original que lo produjo y sus lat/lon, sin reproyectar el
  punto entero. El polígono de mapa difiere como máximo por el redondeo de cada nodo.
- Python solo verifica y mide SciPy: cargar todos los puntos como ndarray float64
  fuera del reloj, contrastar los cuatro conjuntos ANTES de medir cualquiera, comparar
  conjuntos de vértices y orden CCW canónico de Java. Sin QJ (no perturbar puntos).
  Error de Qhull o discrepancia aborta sin modificar envolvente.csv. Los cuatro
  conjuntos del experimento deben tener >=3 puntos no colineales. Degenerados del
  algoritmo propio se comprueban con JUnit, no se fuerzan opciones de Qhull.
- Medir ConvexHull directamente con medir_con_limite, ndarray preparado, un proceso
  spawn por conjunto. Validar de nuevo sus vértices tras cada serie completada.
  Guardar máquina Python en results/r3/maquina_python.csv sin pisar evidencia R1.
- Tabla final mantiene columnas base del anexo y amplía: conjunto,n,vertices,
  tiempo_propio_ms,tiempo_scipy_ms,cociente,coinciden,repeticiones,semilla,
  repeticiones_scipy,estado_propio,estado_scipy. repeticiones corresponde a Java;
  cociente solo con ambas completadas y tiempo SciPy >0. Timeout no inventa tiempo.
- Figura opcional aprobada results/figuras/r3_tiempos.png: barras por conjunto y método,
  eje y ms; no se unen tamaños de distribuciones distintas. Timeouts sin barras de cero.

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
| T3-i | duplicados, empates máximos, segmentos verticales y orden inverso | lista canónica sin colineales, entrada intacta | manual/oráculo |
| T3-j | coordenadas UTM grandes y operaciones que desbordan | giro exacto o error explícito | borde numérico |
| T3-k | null, coordenadas no finitas o tamaños distintos | IllegalArgumentException | validación |
| T3.I-a | red sintética y medidor sustituido | cuatro conjuntos reproducibles, CSV y mapa correctos | integración |
| T3.I-b | resultado medido incorrecto, timeout, red inválida | error sin sobrescritura; timeout explícito | integración |
| T3.V-a | cuatro conjuntos sintéticos Java | SciPy verifica antes de medir; tabla/cociente/máquina | SciPy real |
| T3.V-b | vértices/puntos/conteos/estados incorrectos o medidor incorrecto | error sin publicar tabla | verificación |
| T3.F-a | tabla final y timeout | figura de ambos métodos sin cifras ficticias | Matplotlib |

## Criterios de aceptación

- [x] T3-* en verde (`EnvolventeConvexaTest`, `CatalogoServiceTest.t3h_...`).
- [x] Verificación contra SciPy en **todas** las pruebas del experimento (columna
      `coinciden` = True en `envolvente.csv`).
- [x] Tabla con los 4 conjuntos: nodos de la red; cuadrado con 1 000, 10 000 y 50 000 puntos.
- [x] Recurrencia, cotas mejor/equilibrada/general, promedio O(n·E[h]), pila y memoria derivadas; no se
      confunde la cota equilibrada con una esperanza probada para toda distribución.
- [x] Fuente de SciPy/Qhull verificada, comparación de trabajo y límites de causalidad.
- [x] Figura, salida geográfica para R5 y guía personal de sustentación actualizadas.

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

## Decisiones resueltas y política de discrepancias

1. **Algoritmo**: **QuickHull**, aprobado al continuar el bloque (2026-11-12; divide y vencerás; descarta pronto
   los puntos interiores, que en la red son la gran mayoría; es la misma idea que usa Qhull,
   lo que hace interesante la comparación). Alternativa con peor caso garantizado
   O(n log n): envolvente por mezcla (Preparata y Hong, 1977), T(n) = 2T(n/2) + O(n).
    Se documenta la profundidad de recursión en el peor caso (`bootRun` usa
   `-Xss16m`).
2. Si SciPy y el algoritmo propio difieren en puntos colineales, investigar las opciones de
   Qhull y documentarlo (no "arreglar" el resultado propio para que coincida).
