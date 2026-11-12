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
    if not set(columnas).issubset(filas.columns):
        raise ValueError(f"Faltan columnas numéricas: {columnas}")
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
    nodos = _leer(DATA_PROCESSED / "nodos.csv.gz", ["idx"], "idx")
    indices = _numeros(nodos, ["idx"]).ravel()
    if sorted(indices) != list(range(len(nodos))):
        raise ValueError("nodos.idx debe ser 0..V-1")
    catalogo = _leer(CATALOGO, ["id"], "id")
    asociaciones = _leer(DATA_PROCESSED / "lugares_nodos.csv",
                         ["id_lugar", "idx_nodo"], "id_lugar")
    ids = catalogo.id.tolist()
    if set(ids) != set(asociaciones.id_lugar):
        raise ValueError("Asociaciones y catálogo no coinciden")
    valores = _numeros(asociaciones, ["idx_nodo"]).ravel()
    if not (np.equal(valores, np.floor(valores)).all()
            and (valores >= 0).all() and (valores < len(nodos)).all()):
        raise ValueError("Índice de asociación inválido")
    por_id = dict(zip(asociaciones.id_lugar, valores.astype(int)))
    aristas = pd.read_csv(DATA_PROCESSED / "aristas.csv.gz")
    datos = _numeros(aristas, ["u", "v", "tiempo_s"])
    if (len(datos) == 0 or (datos[:, 2] < 0).any()
            or not np.equal(datos[:, :2], np.floor(datos[:, :2])).all()
            or (datos[:, :2] < 0).any() or (datos[:, :2] >= len(nodos)).any()):
        raise ValueError("Aristas o pesos inválidos")
    # MultiDiGraph conserva paralelas: NetworkX usa el mínimo peso por par dirigido.
    grafo = nx.MultiDiGraph()
    grafo.add_nodes_from(range(len(nodos)))
    for u, destino, peso in datos:
        grafo.add_edge(int(u), int(destino), tiempo_s=float(peso))

    carpeta = RESULTS / "r2"
    pares = pd.read_csv(carpeta / "pares.csv")
    requeridas = {"origen", "destino", "nodo_origen", "nodo_destino", "medir_lineal"}
    if pares.empty or not requeridas.issubset(pares.columns) or pares.duplicated(["origen", "destino"]).any():
        raise ValueError("Pares vacíos, repetidos o sin columnas requeridas")
    esperadas = set()
    for fila in pares.itertuples(index=False):
        if fila.origen not in por_id or fila.destino not in por_id:
            raise ValueError("ID de par desconocido")
        if fila.nodo_origen != por_id[fila.origen] or fila.nodo_destino != por_id[fila.destino]:
            raise ValueError("Nodos de par no coinciden con asociaciones")
        bandera = str(fila.medir_lineal).lower()
        if bandera not in ("true", "false"):
            raise ValueError("medir_lineal debe ser true o false")
        esperadas.add((fila.origen, fila.destino, "cola_prioridad"))
        if bandera == "true":
            esperadas.add((fila.origen, fila.destino, "lineal"))
    mediciones = pd.read_csv(carpeta / "mediciones_dijkstra.csv", dtype={"ruta_nodos": str})
    columnas = {"origen", "destino", "version", "tiempo_viaje_s", "nodos_explorados",
                "tiempo_ms", "repeticiones", "estado", "ruta_nodos"}
    if (not columnas.issubset(mediciones.columns) or mediciones.empty
            or mediciones.duplicated(["origen", "destino", "version"]).any()):
        raise ValueError("Mediciones vacías, duplicadas o sin columnas requeridas")
    claves = set(mediciones[["origen", "destino", "version"]].itertuples(index=False, name=None))
    if claves != esperadas:
        raise ValueError("Mediciones y pares no coinciden")

    # Caché de oráculo de librería, fuera de cualquier medición. No produce la matriz Java.
    distancias = {identificador: nx.single_source_dijkstra_path_length(
        grafo, por_id[identificador], weight="tiempo_s") for identificador in ids}
    filas = []
    for fila in mediciones.itertuples(index=False):
        if fila.estado == "no completada":
            if not all(pd.isna(valor) for valor in (fila.tiempo_viaje_s, fila.nodos_explorados,
                                                    fila.tiempo_ms, fila.ruta_nodos)):
                raise ValueError("Una medición no completada contiene resultados ficticios")
            continue
        if fila.estado != "completada":
            raise ValueError("Estado de medición desconocido")
        numeros = [fila.tiempo_viaje_s, fila.nodos_explorados, fila.tiempo_ms, fila.repeticiones]
        if not all(math.isfinite(float(valor)) and float(valor) >= 0 for valor in numeros):
            raise ValueError("Medición con números inválidos")
        if (fila.nodos_explorados != int(fila.nodos_explorados)
                or not 1 <= fila.nodos_explorados <= len(nodos)
                or fila.repeticiones != int(fila.repeticiones) or fila.repeticiones < 1):
            raise ValueError("Cantidad de explorados o repeticiones inválida")
        try:
            ruta = [int(nodo) for nodo in str(fila.ruta_nodos).split("|")]
        except ValueError as error:
            raise ValueError("Ruta con índices inválidos") from error
        ruta_valida = (len(ruta) > 0 and ruta[0] == por_id[fila.origen]
                       and ruta[-1] == por_id[fila.destino] and len(ruta) == len(set(ruta))
                       and len(ruta) <= fila.nodos_explorados)
        costo = 0.0
        for u, destino in zip(ruta, ruta[1:]):
            if not grafo.has_edge(u, destino):
                ruta_valida = False
                break
            costo += min(arista["tiempo_s"] for arista in grafo[u][destino].values())
        esperado = distancias[fila.origen].get(por_id[fila.destino], math.inf)
        coincide = (ruta_valida and math.isfinite(esperado)
                    and math.isclose(float(fila.tiempo_viaje_s), esperado, abs_tol=1e-6, rel_tol=0)
                    and math.isclose(costo, esperado, abs_tol=1e-6, rel_tol=0))
        filas.append((fila.origen, fila.destino, fila.tiempo_viaje_s, esperado, coincide))

    matriz = pd.read_csv(carpeta / "matriz_tiempos.csv", index_col="id")
    if (list(matriz.index) != list(matriz.columns) or set(matriz.index) != set(ids)
            or matriz.index.duplicated().any() or matriz.columns.duplicated().any()):
        raise ValueError("IDs de matriz no coinciden con el catálogo o su orden de columnas")
    valores_matriz = matriz.to_numpy(dtype=float)
    if not np.isfinite(valores_matriz).all() or (valores_matriz < 0).any():
        raise ValueError("Matriz no finita o con tiempos negativos")
    filas_matriz = []
    for origen in matriz.index:
        for destino in matriz.columns:
            java = float(matriz.loc[origen, destino])
            esperado = distancias[origen].get(por_id[destino], math.inf)
            coincide = math.isfinite(esperado) and math.isclose(java, esperado, abs_tol=0.0005001, rel_tol=0)
            if origen == destino:
                coincide = coincide and java == 0
            filas_matriz.append((origen, destino, java, esperado, coincide))
    columnas_salida = ["origen", "destino", "tiempo_java_s", "tiempo_networkx_s", "coinciden"]
    pd.DataFrame(filas, columns=columnas_salida).to_csv(carpeta / "verificacion_networkx.csv", index=False)
    pd.DataFrame(filas_matriz, columns=columnas_salida).to_csv(
        carpeta / "verificacion_matriz_networkx.csv", index=False)
    if not all(fila[-1] for fila in filas + filas_matriz):
        raise ValueError(f"Discrepancias con NetworkX; revisar reportes en {carpeta}")
