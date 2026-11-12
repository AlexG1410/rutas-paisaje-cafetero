"""R1.3 — Tiempo de ``ox.truncate.largest_component`` en redes de tamaño creciente.

Spec: specs/R1-modelo-red.md. Redes: ``ox.graph_from_point`` desde la plaza de Bolívar de
Armenia (coordenadas de ``cab_armenia`` en el catálogo) con radios RADIOS_KM, más la red del
departamento. Se medirá con ``preparacion.medicion.medir_con_limite`` (5 repeticiones,
mediana y corte por ejecución). La adquisición y verificación quedan fuera del tiempo.

Salidas:
- results/r1/tamano_red.csv          red, nodos, aristas  (descargada y componente)
- results/r1/tiempo_componente.csv   red, V, E, V_mas_E, tiempo_ms, repeticiones, estado
"""

from __future__ import annotations

import json
import math
from datetime import datetime, timezone
from pathlib import Path
from tempfile import NamedTemporaryFile
from xml.etree.ElementTree import ParseError

import networkx as nx
import osmnx as ox
import pandas as pd

from preparacion import red
from preparacion.config import CATALOGO, DATA_RAW, ID_PLAZA_BOLIVAR, RADIOS_KM, RESULTS
from preparacion.maquina import guardar_info_maquina
from preparacion.medicion import medir_con_limite


def leer_centro(ruta: Path) -> tuple[float, float]:
    """Lee el centro aprobado del catálogo sin proponer otras coordenadas."""
    tabla = pd.read_csv(ruta)
    filas = tabla.loc[tabla["id"] == ID_PLAZA_BOLIVAR]
    if len(filas) != 1:
        raise ValueError("El catálogo debe contener un único cab_armenia")
    lat, lon = float(filas.iloc[0]["lat"]), float(filas.iloc[0]["lon"])
    if not (math.isfinite(lat) and math.isfinite(lon) and
            -90 <= lat <= 90 and -180 <= lon <= 180):
        raise ValueError("El centro no tiene coordenadas geográficas válidas")
    return lat, lon


def _leer_red(ruta: Path, opciones: dict) -> nx.MultiDiGraph:
    """Valida una copia sin modificarla ni redescargar ante errores."""
    try:
        grafo = ox.load_graphml(ruta)
        red._validar_grafo(grafo)
        meta = json.loads(grafo.graph["metadatos_descarga"])
        if not isinstance(meta, dict):
            raise ValueError("Faltan metadatos de descarga válidos")
        esperados = dict(opciones, nodos_descargada=grafo.number_of_nodes(),
                         aristas_descargada=grafo.number_of_edges())
        if any(meta.get(clave) != valor for clave, valor in esperados.items()):
            raise ValueError("La procedencia o los conteos no coinciden")
        fecha = datetime.fromisoformat(meta["fecha_descarga"])
        if fecha.utcoffset() is None or fecha.utcoffset().total_seconds() != 0:
            raise ValueError("La fecha debe indicar UTC")
        for biblioteca in ("osmnx", "networkx"):
            if not isinstance(meta.get(biblioteca), str) or not meta[biblioteca]:
                raise ValueError(f"Falta versión {biblioteca}")
        if not isinstance(meta.get("uso_cache_http"), bool):
            raise ValueError("Falta uso_cache_http")
        return grafo
    except (OSError, ValueError, KeyError, TypeError, ParseError, nx.NetworkXException) as error:
        raise ValueError(f"No se puede reutilizar {ruta}: {error}; conserve la copia") from error


def preparar_red(ruta: Path, centro: tuple[float, float], radio_km: int) -> nx.MultiDiGraph:
    """Adquiere un cuadrado vial o reutiliza GraphML validado; nunca sobrescribe."""
    opciones = {"centro": list(centro), "radio_km": radio_km, "dist_type": "bbox",
                "network_type": "drive", "retain_all": True, "simplify": True}
    if ruta.exists():
        return _leer_red(ruta, opciones)
    grafo = ox.graph_from_point(centro, dist=radio_km * 1000, dist_type="bbox",
                                network_type="drive", retain_all=True, simplify=True)
    red._validar_grafo(grafo)
    meta = dict(opciones, fecha_descarga=datetime.now(timezone.utc).isoformat(timespec="seconds"),
                osmnx=ox.__version__, networkx=nx.__version__,
                nodos_descargada=grafo.number_of_nodes(), aristas_descargada=grafo.number_of_edges(),
                uso_cache_http=bool(ox.settings.use_cache))
    grafo.graph["metadatos_descarga"] = json.dumps(meta, ensure_ascii=False)
    ruta.parent.mkdir(parents=True, exist_ok=True)
    with NamedTemporaryFile(dir=ruta.parent, suffix=".graphml.tmp", delete=False) as archivo:
        temporal = Path(archivo.name)
    try:
        ox.save_graphml(grafo, filepath=temporal)
        recuperada = _leer_red(temporal, opciones)
        if ruta.exists():
            raise FileExistsError(f"Apareció {ruta}; no se sobrescribirá")
        temporal.rename(ruta)
        return recuperada
    finally:
        temporal.unlink(missing_ok=True)


def validar_componente(grafo: nx.MultiDiGraph, componente: nx.MultiDiGraph) -> None:
    """Oráculo de librería independiente: Kosaraju y subgrafo inducido con paralelas."""
    particion = list(nx.kosaraju_strongly_connected_components(grafo))
    nodos = set(componente.nodes)
    mayor = max(map(len, particion))
    if len(nodos) != mayor or not any(nodos == grupo for grupo in particion):
        raise ValueError("La SCC no es una componente fuerte de tamaño máximo")
    esperado = grafo.subgraph(nodos)
    if set(componente.edges(keys=True)) != set(esperado.edges(keys=True)):
        raise ValueError("La SCC no conserva las aristas del subgrafo inducido")
    if not nx.is_strongly_connected(componente):
        raise ValueError("La SCC no es fuertemente conexa")


def verificar(grafo: nx.MultiDiGraph) -> nx.MultiDiGraph:
    """Contrasta la partición de NetworkX y el resultado OSMnx antes de medir."""
    tarjan = {frozenset(grupo) for grupo in nx.strongly_connected_components(grafo)}
    kosaraju = {frozenset(grupo) for grupo in nx.kosaraju_strongly_connected_components(grafo)}
    if tarjan != kosaraju:
        raise ValueError("Las particiones SCC de Tarjan y Kosaraju difieren")
    componente = ox.truncate.largest_component(grafo, strongly=True)
    validar_componente(grafo, componente)
    return componente


def ejecutar() -> None:
    """Prepara y verifica todo antes de medir; no modifica la red de producción."""
    centro = leer_centro(CATALOGO)
    departamento = red._validar_guardada(DATA_RAW / "quindio.graphml")
    redes = [(f"armenia_{radio}km", preparar_red(
        DATA_RAW / "r1" / f"armenia_{radio}km.graphml", centro, radio,
    )) for radio in RADIOS_KM]
    redes.append(("quindio", departamento))
    tamanos = []
    for nombre, grafo in redes:
        componente = verificar(grafo)
        for sufijo, version in (("descargada", grafo), ("componente", componente)):
            tamanos.append({"red": f"{nombre}_{sufijo}", "nodos": version.number_of_nodes(),
                            "aristas": version.number_of_edges()})
    RESULTS.mkdir(parents=True, exist_ok=True)
    guardar_info_maquina(RESULTS / "maquina_python.csv")
    tiempos = []
    for nombre, grafo in redes:
        medicion = medir_con_limite(ox.truncate.largest_component, grafo, strongly=True)
        if medicion.completada:
            validar_componente(grafo, medicion.resultado)
        v, e = grafo.number_of_nodes(), grafo.number_of_edges()
        tiempos.append({"red": nombre, "V": v, "E": e, "V_mas_E": v + e,
                        **medicion.como_fila()})
    salida = RESULTS / "r1"
    salida.mkdir(parents=True, exist_ok=True)
    pd.DataFrame(tamanos, columns=["red", "nodos", "aristas"]).to_csv(
        salida / "tamano_red.csv", index=False)
    pd.DataFrame(tiempos, columns=["red", "V", "E", "V_mas_E", "tiempo_ms",
                                 "repeticiones", "estado"]).to_csv(
        salida / "tiempo_componente.csv", index=False)
