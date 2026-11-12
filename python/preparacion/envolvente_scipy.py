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

import math
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.spatial import ConvexHull, QhullError

from preparacion.config import RESULTS, SEMILLA
from preparacion.maquina import guardar_info_maquina
from preparacion.medicion import medir_con_limite

CONJUNTOS = ("red", "cuadrado_1000", "cuadrado_10000", "cuadrado_50000")
COLUMNAS_PROPIO = ("conjunto", "n", "vertices", "tiempo_propio_ms", "repeticiones", "semilla", "estado")
COLUMNAS_FINAL = ("conjunto", "n", "vertices", "tiempo_propio_ms", "tiempo_scipy_ms", "cociente",
                  "coinciden", "repeticiones", "semilla", "repeticiones_scipy", "estado_propio", "estado_scipy")


def _leer_puntos(ruta: Path) -> np.ndarray:
    """Datos enteros exactos en el dominio experimental, sin deduplicar ni reordenar."""
    tabla = pd.read_csv(ruta)
    if list(tabla.columns) != ["x", "y"] or tabla.empty:
        raise ValueError(f"Puntos vacíos o columnas inválidas: {ruta}")
    puntos = tabla.apply(pd.to_numeric, errors="raise").to_numpy(dtype=np.float64)
    if (not np.isfinite(puntos).all() or not np.equal(puntos, np.floor(puntos)).all()
            or (np.abs(puntos) > 2**53 - 1).any() or (np.ptp(puntos, axis=0) > 1e9).any()
            or tabla.duplicated().any()):
        raise ValueError(f"Coordenadas inválidas, repetidas o fuera del rango experimental: {ruta}")
    return np.ascontiguousarray(puntos)


def _vertices(puntos: np.ndarray, casco: ConvexHull) -> list[tuple[int, int]]:
    """Lee el orden CCW de SciPy y rota al mínimo; no implementa otro algoritmo geométrico."""
    if not isinstance(casco, ConvexHull) or not np.array_equal(casco.points, puntos):
        raise ValueError("Medición SciPy sin resultado ConvexHull")
    vertices = [tuple(map(int, punto)) for punto in puntos[casco.vertices]]
    primero = vertices.index(min(vertices))
    return vertices[primero:] + vertices[:primero]


def _entero(valor: float, minimo: int) -> bool:
    return math.isfinite(float(valor)) and valor >= minimo and valor == int(valor)


def ejecutar() -> None:
    """Verifica los cuatro conjuntos antes de medir; solo publica una tabla validada."""
    carpeta = RESULTS / "r3"
    tabla = pd.read_csv(carpeta / "envolvente_propio.csv")
    if (list(tabla.columns) != list(COLUMNAS_PROPIO) or len(tabla) != len(CONJUNTOS)
            or tabla.conjunto.duplicated().any() or set(tabla.conjunto) != set(CONJUNTOS)):
        raise ValueError("Tabla Java no contiene exactamente los cuatro conjuntos R3")
    preparados = {}
    for fila in tabla.itertuples(index=False):
        if (not _entero(fila.n, 3) or not _entero(fila.vertices, 3)
                or fila.vertices > fila.n or fila.semilla != SEMILLA
                or not _entero(fila.repeticiones, 0)
                or fila.estado not in ("completada", "no completada")):
            raise ValueError(f"Conteos, semilla o estado inválidos: {fila.conjunto}")
        if fila.estado == "completada":
            if (not math.isfinite(float(fila.tiempo_propio_ms)) or fila.tiempo_propio_ms < 0
                    or fila.repeticiones < 1):
                raise ValueError("Tiempo o repeticiones Java inválidos")
        elif not pd.isna(fila.tiempo_propio_ms):
            raise ValueError("Timeout Java con tiempo ficticio")
        puntos = _leer_puntos(carpeta / f"puntos_{fila.conjunto}.csv")
        java = [tuple(map(int, punto)) for punto in _leer_puntos(
            carpeta / f"vertices_{fila.conjunto}.csv")]
        if len(puntos) != fila.n or len(java) != fila.vertices:
            raise ValueError("Tamaños de puntos/vértices no coinciden con tabla Java")
        try:
            referencia = ConvexHull(puntos)  # Oráculo, no es una medición.
        except QhullError as error:
            raise ValueError(f"SciPy no resuelve {fila.conjunto}; revisar degeneración/precisión") from error
        if java != _vertices(puntos, referencia):
            raise ValueError(f"Discrepancia de vértices u orden canónico: {fila.conjunto}")
        preparados[fila.conjunto] = (puntos, java)

    guardar_info_maquina(carpeta / "maquina_python.csv")
    filas = []
    for fila in tabla.itertuples(index=False):
        puntos, java = preparados[fila.conjunto]
        medicion = medir_con_limite(ConvexHull, puntos)
        scipy_ms = medicion.mediana_ms
        if medicion.completada:
            if (scipy_ms is None or not math.isfinite(scipy_ms) or scipy_ms < 0
                    or _vertices(puntos, medicion.resultado) != java):
                raise ValueError(f"Resultado medido SciPy incorrecto: {fila.conjunto}")
        cociente = None
        if fila.estado == "completada" and scipy_ms is not None and scipy_ms > 0:
            cociente = fila.tiempo_propio_ms / scipy_ms
            if not math.isfinite(cociente):
                raise ValueError("Cociente no finito")
        filas.append((fila.conjunto, fila.n, fila.vertices, fila.tiempo_propio_ms,
                      scipy_ms, cociente, True, fila.repeticiones, fila.semilla,
                      medicion.repeticiones, fila.estado,
                      "completada" if medicion.completada else "no completada"))
    pd.DataFrame(filas, columns=COLUMNAS_FINAL).to_csv(carpeta / "envolvente.csv", index=False)
