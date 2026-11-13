# 5. R4 — Ruta óptima de recorrido

## 5.1 Problema (TSP asimétrico)

La partida se mantiene fija; se visitan k lugares y se regresa minimizando la suma
de tiempos de la matriz R2. La matriz es dirigida: invertir un recorrido puede
cambiar su costo. Los índices de `lugares` remiten a filas y columnas del catálogo;
no necesitan ser consecutivos. Las visitas turísticas se suman después de ordenar:
para un conjunto fijo, su duración no depende del orden.

## 5.2 Fuerza bruta: complejidad

Se generan las k! permutaciones con intercambios recursivos propios. Cada hoja suma
los k+1 desplazamientos, incluido el regreso. No se poda ni se identifican recorridos
inversos, pues la matriz es asimétrica. En empate se conserva la primera hoja.
La partida permanece fija y en cada nivel se fija un destino adicional; al regresar,
se deshace el intercambio. Ese invariante permite enumerar cada permutación una vez.

El número de nodos de generación satisface `G(k)=k·G(k−1)+O(k)` y tiene k! hojas.
Sumar cada recorrido completo cuesta Θ(k): tiempo Θ(k·k!). La copia del recorrido,
el mejor recorrido y la pila de profundidad k ocupan O(k). Estas cotas se derivan
del código; la enumeración del TSP se presenta en la guía 09, sección 9.

## 5.3 Held-Karp: subestructura óptima, recurrencia, complejidad y memoria de la tabla

**Subestructura óptima.** Si un camino óptimo parte de p, visita S y termina en j,
y su penúltimo lugar es i, el prefijo hasta i debe ser óptimo para `(S−{j},i)`.
Si hubiera un prefijo más barato, sustituirlo conservaría los mismos lugares y el
último arco i→j y produciría un camino de menor costo, contradicción. Distintos
órdenes comparten estados `(S,j)`, lo que permite reutilizar subproblemas.

Con la partida implícita, `dp[S][j]` guarda el menor costo de visitar S y terminar
en j. Base `dp[{j}][j]=M[p][j]`; transición
`dp[S][j]=min_{i∈S−{j}}(dp[S−{j}][i]+M[i][j])`; cierre
`OPT=min_j(dp[U][j]+M[j][p])`. Se guardan predecesores para reconstruir la ruta.
Las máscaras tienen un bit por posición local, no por índice global del catálogo.
Al quitar un bit se obtiene una máscara menor; recorrerlas crecientemente respeta
las dependencias. En empate se conserva el primer anterior; el cierre considera
finales en el orden de entrada. Dos algoritmos exactos pueden dar rutas distintas
con el mismo costo óptimo.

Hay k·2^k celdas reservadas, incluidas combinaciones sin j∈S. Las transiciones
examinan k(k−1)·2^(k−2) pares válidos para k≥2 y los barridos añaden O(k²·2^k):
tiempo Θ(k²·2^k), inicialización Θ(k·2^k), reconstrucción O(k). Dos arreglos planos
guardan un double y un int por celda: 12·k·2^k bytes de carga útil. La memoria es
Θ(k·2^k), incluida salida; la reconstrucción es iterativa. Se excluyen cabeceras y
alineación de arreglos de la estimación, así como la matriz de entrada compartida.
La recurrencia es por subconjuntos, no de la forma del Teorema Maestro.
La guía 09, sección 9, explica el estado y el orden O(n²·2^n); el conteo anterior y
la estimación corresponden a nuestra implementación con k visitas.

Los tres métodos validan forma de la matriz en O(N) y pesos seleccionados en O(k²),
con memoria constante adicional. Se exige diagonal cero y tiempos finitos no negativos.
Held-Karp limita la resolución a 20 visitas por recursos; el experimento usa hasta 12.

## 5.4 Verificación

## 5.5 Resultados de las soluciones exactas

| k | Tiempo fuerza bruta (ms) | Tiempo Held-Karp (ms) |
|---|---|---|
| 5 | … | … |
| … | … | … |
| 12 | … | … |

Figura: k vs tiempo (ms). Mayor k resuelto por cada método.

## 5.6 Heurística voraz: elección y justificación

Se eligió vecino más cercano por su sencillez y costo bajo. Sus elementos, siguiendo
la guía 08, son: candidatos = destinos pendientes; selección = menor tiempo desde el
actual; factibilidad = destino no visitado; solución = todos visitados más regreso;
objetivo = minimizar desplazamientos. Los empates siguen el orden de entrada.
Los barridos suman k+(k−1)+…+1=Θ(k²); visitados y recorrido ocupan O(k).
La decisión local no considera los arcos que quedarán al final: se evalúa su calidad
frente a Held-Karp en lugar de suponer optimalidad.
## 5.7 Calidad de la heurística

| k | Instancias | Brecha promedio (%) | Brecha máxima (%) |
|---|---|---|---|
| 5 | … | … | … |
| 8 | … | … | … |
| 10 | … | … | … |
| 12 | … | … | … |

## 5.8 Itinerario de un día (lugar de partida: …)

| Orden | Lugar | Desplazamiento desde el anterior (min) | Visita (min) | Tiempo acumulado (h) |
|---|---|---|---|---|
| 1 | … | … | … | … |
| Regreso | Lugar de partida | … | — | … |

Totales: visitas …, desplazamientos …, jornada …. Atractivos retirados y criterio.

## 5.9 Voraz frente a óptimo (y comparación con Dijkstra)
