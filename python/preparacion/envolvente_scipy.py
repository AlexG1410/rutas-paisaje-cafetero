"""R3 — SciPy sobre los MISMOS puntos que usó Java (spec: specs/R3-envolvente-convexa.md).

Para cada conjunto de results/r3/envolvente_propio.csv:
1. Leer results/r3/puntos_<conjunto>.csv y construir el ``numpy.ndarray`` (fuera de la
   medición).
2. Verificar: vértices de ``scipy.spatial.ConvexHull`` == results/r3/vertices_<conjunto>.csv
   (como conjuntos).
3. Medir ``ConvexHull(arreglo)`` con ``preparacion.medicion.medir_con_limite``.
4. Escribir results/r3/envolvente.csv: conjunto, n, vertices, tiempo_propio_ms,
   tiempo_scipy_ms, cociente, coinciden, repeticiones, semilla.
"""

from __future__ import annotations


def ejecutar() -> None:
    raise NotImplementedError("R3 — ver specs/R3-envolvente-convexa.md")
