"""Verificación (protocolo 6.6) de resultados de Java contra librerías de Python.

NetworkX y OSMnx solo se usan aquí para COMPROBAR, nunca para producir resultados.
"""

from __future__ import annotations

import math
from pathlib import Path

import networkx as nx
import numpy as np
import osmnx as ox
import pandas as pd

from preparacion.config import CATALOGO, DATA_PROCESSED, EPSG_UTM_18N, RESULTS
from preparacion.proyeccion import a_utm_lote


def _leer(ruta: Path, columnas: list[str], id_columna: str) -> pd.DataFrame:
    """Exige cabecera, filas e identificadores únicos antes de contrastar resultados."""
    filas = pd.read_csv(ruta)
    if filas.empty or not set(columnas).issubset(filas.columns):
        raise ValueError(f"Archivo vacío o columnas faltantes: {ruta}")
    ids = filas[id_columna]
    if ids.isna().any() or ids.astype(str).str.strip().eq("").any() or ids.duplicated().any():
        raise ValueError(f"IDs vacíos o repetidos: {ruta}")
    return filas


def _numeros(filas: pd.DataFrame, columnas: list[str]) -> np.ndarray:
    """Convierte números y rechaza NaN e infinitos, sin corregir datos silenciosamente."""
    valores = filas[columnas].apply(pd.to_numeric, errors="raise").to_numpy(dtype=float)
    if not np.isfinite(valores).all():
        raise ValueError(f"Valores no finitos en {columnas}")
    return valores


def verificar_lugares() -> None:
    """R1.4: compara data/processed/lugares_nodos.csv (NodoMasCercano en Java) con
    ``ox.distance.nearest_nodes`` sobre el grafo proyectado.
    Salida: results/r1/verificacion_lugar_nodo.csv (id_lugar, idx_java, idx_osmnx, coinciden).
    """
    nodos = _leer(DATA_PROCESSED / "nodos.csv.gz", ["idx", "x_utm", "y_utm"], "idx")
    catalogo = _leer(CATALOGO, ["id", "lat", "lon"], "id")
    utm = _leer(DATA_PROCESSED / "lugares_utm.csv",
                ["id_lugar", "x_utm", "y_utm"], "id_lugar")
    java = _leer(DATA_PROCESSED / "lugares_nodos.csv",
                 ["id_lugar", "idx_nodo", "distancia_m", "mas_de_500m", "tratamiento"],
                 "id_lugar")
    ids = catalogo.id.tolist()
    if set(ids) != set(utm.id_lugar) or set(ids) != set(java.id_lugar):
        raise ValueError("Los IDs de catálogo, UTM y asociaciones no coinciden")
    utm = utm.set_index("id_lugar").loc[ids]
    java = java.set_index("id_lugar").loc[ids]
    coordenadas = _numeros(nodos, ["x_utm", "y_utm"])
    indices = _numeros(nodos, ["idx"]).ravel()
    if sorted(indices) != list(range(len(nodos))):
        raise ValueError("nodos.idx debe ser 0..V-1")
    puntos = _numeros(utm, ["x_utm", "y_utm"])
    latlon = _numeros(catalogo, ["lat", "lon"])
    xs, ys = a_utm_lote(latlon[:, 0], latlon[:, 1])
    if not np.allclose(puntos, np.column_stack((xs, ys)), atol=1e-6, rtol=0):
        raise ValueError("Coordenadas UTM desactualizadas respecto al catálogo")
    resultados_java = _numeros(java, ["idx_nodo", "distancia_m"])
    indices_java = resultados_java[:, 0]
    distancias_java = resultados_java[:, 1]
    if (not np.equal(indices_java, np.floor(indices_java)).all()
            or (indices_java < 0).any() or (indices_java >= len(nodos)).any()
            or (distancias_java < 0).any()):
        raise ValueError("Índice Java fuera de rango o distancia negativa")
    banderas = java.mas_de_500m.astype(str).str.lower()
    if not banderas.isin(["true", "false"]).all():
        raise ValueError("mas_de_500m debe ser true o false")

    # OSMnx solo necesita nodos con x/y y CRS para su consulta espacial; no se descarga red.
    grafo = nx.MultiDiGraph(crs=f"EPSG:{EPSG_UTM_18N}")
    for indice, (x, y) in zip(indices, coordenadas):
        grafo.add_node(int(indice), x=float(x), y=float(y))
    indices_ox, distancias_ox = ox.distance.nearest_nodes(
        grafo, X=puntos[:, 0], Y=puntos[:, 1], return_dist=True)
    filas = []
    for i, identificador in enumerate(ids):
        coincide = (
            int(indices_java[i]) == int(indices_ox[i])
            and math.isclose(float(distancias_java[i]), float(distancias_ox[i]),
                             abs_tol=1e-6, rel_tol=0)
            and (banderas.iloc[i] == "true") == (distancias_java[i] > 500)
        )
        filas.append((identificador, int(indices_java[i]), int(indices_ox[i]), coincide))
    salida = RESULTS / "r1/verificacion_lugar_nodo.csv"
    salida.parent.mkdir(parents=True, exist_ok=True)
    pd.DataFrame(filas, columns=["id_lugar", "idx_java", "idx_osmnx", "coinciden"]).to_csv(
        salida, index=False)
    if not all(fila[3] for fila in filas):
        raise ValueError(f"Discrepancias con OSMnx; revisar {salida}")


def verificar_dijkstra() -> None:
    """R2: para cada par de results/r2/mediciones_dijkstra.csv, recalcula el tiempo con
    ``nx.dijkstra_path_length`` (peso tiempo_s) sobre la red procesada.
    Salida: results/r2/verificacion_networkx.csv (origen, destino, tiempo_java_s,
    tiempo_networkx_s, coinciden).
    """
    raise NotImplementedError("R2 — ver specs/R2-caminos-minimos.md")
