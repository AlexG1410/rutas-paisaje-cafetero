"""R1 — Descarga, pesos, componente fuertemente conexa y exportación para Java.

Spec: specs/R1-modelo-red.md. Aquí SÍ se usan librerías (OSMnx, pyproj, pandas): el
enunciado exige descargar en Python y exportar a archivos si los algoritmos van en Java.

Contrato de salida (lo lee co.edu.uniquindio.rutas.repository.RedRepository):
    data/processed/nodos.csv.gz     idx, osmid, lat, lon, x_utm, y_utm   (idx = 0..V-1)
    data/processed/aristas.csv.gz   u, v, longitud_m, velocidad_kmh, fuente_velocidad,
                                    tiempo_s, highway                     (u, v en idx)
    data/processed/lugares_utm.csv  id_lugar, x_utm, y_utm
    data/processed/metadatos.json   fecha_descarga, consulta, versiones, conteos, ...
"""

from __future__ import annotations

import json
import math
import re
from datetime import datetime, timezone
from pathlib import Path
from tempfile import NamedTemporaryFile
from typing import Any
from xml.etree.ElementTree import ParseError

import networkx as nx
import osmnx as ox
import pandas as pd

from preparacion.config import (
    CATALOGO,
    DATA_PROCESSED,
    DATA_RAW,
    LUGAR_OSM,
    RETENER_TODAS_COMPONENTES,
    SIMPLIFICAR_RED,
    TIPO_RED,
)
from preparacion.proyeccion import a_utm_lote

COLUMNAS_NODOS = ["idx", "osmid", "lat", "lon", "x_utm", "y_utm"]
COLUMNAS_ARISTAS = [
    "u", "v", "longitud_m", "velocidad_kmh", "fuente_velocidad", "tiempo_s", "highway",
]


def descargar() -> None:
    """Adquiere o reutiliza la red cruda según la sección Descarga de la spec R1.

    Conserva todas las componentes de vías para vehículos. La fecha y procedencia van
    dentro del GraphML, en ``metadatos_descarga`` (JSON). Nunca sobrescribe una copia
    existente: exige que pueda leerse y que corresponda a la configuración aprobada.
    """
    archivo = DATA_RAW / "quindio.graphml"
    if archivo.exists():
        _validar_guardada(archivo)
        return

    grafo = ox.graph_from_place(
        LUGAR_OSM,
        network_type=TIPO_RED,
        retain_all=RETENER_TODAS_COMPONENTES,
        simplify=SIMPLIFICAR_RED,
    )
    _validar_grafo(grafo)
    metadatos = {
        "fecha_descarga": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "consulta": LUGAR_OSM,
        "network_type": TIPO_RED,
        "retain_all": RETENER_TODAS_COMPONENTES,
        "simplify": SIMPLIFICAR_RED,
        "osmnx": ox.__version__,
        "networkx": nx.__version__,
        "nodos_descargada": grafo.number_of_nodes(),
        "aristas_descargada": grafo.number_of_edges(),
        "uso_cache_http": bool(ox.settings.use_cache),
    }
    grafo.graph["metadatos_descarga"] = json.dumps(metadatos, ensure_ascii=False)

    DATA_RAW.mkdir(parents=True, exist_ok=True)
    with NamedTemporaryFile(
        dir=DATA_RAW, prefix="quindio-", suffix=".graphml.tmp", delete=False,
    ) as salida:
        temporal = Path(salida.name)
    try:
        ox.save_graphml(grafo, filepath=temporal)
        _validar_guardada(temporal)
        if archivo.exists():
            raise FileExistsError(f"Apareció {archivo}; no se sobrescribirá")
        temporal.rename(archivo)
    finally:
        temporal.unlink(missing_ok=True)


def _validar_grafo(grafo: nx.MultiDiGraph) -> None:
    """Comprueba la forma de la red, sin filtrar componentes ni calcular algoritmos."""
    if not isinstance(grafo, nx.MultiDiGraph):
        raise ValueError("La red debe ser un multigrafo dirigido de OSMnx")
    if grafo.number_of_nodes() == 0 or grafo.number_of_edges() == 0:
        raise ValueError("La red descargada está vacía o no tiene vías")


def _validar_guardada(archivo: Path) -> nx.MultiDiGraph:
    """Lee la copia y verifica su procedencia antes de publicarla o reutilizarla."""
    try:
        grafo = ox.load_graphml(archivo)
        _validar_grafo(grafo)
    except (OSError, ValueError, nx.NetworkXException, ParseError) as error:
        raise ValueError(
            f"No se puede reutilizar {archivo}: {error}. Conserve la copia y revísela; "
            "no se descargará ni sobrescribirá automáticamente."
        ) from error

    try:
        metadatos = json.loads(grafo.graph.get("metadatos_descarga", "null"))
        if not isinstance(metadatos, dict):
            raise ValueError("faltan metadatos_descarga")
        esperados = {
            "consulta": LUGAR_OSM,
            "network_type": TIPO_RED,
            "retain_all": RETENER_TODAS_COMPONENTES,
            "simplify": SIMPLIFICAR_RED,
            "nodos_descargada": grafo.number_of_nodes(),
            "aristas_descargada": grafo.number_of_edges(),
        }
        for campo, esperado in esperados.items():
            if metadatos.get(campo) != esperado:
                raise ValueError(f"{campo} no coincide con la red o configuración")
        fecha = datetime.fromisoformat(metadatos["fecha_descarga"])
        if fecha.utcoffset() is None or fecha.utcoffset().total_seconds() != 0:
            raise ValueError("fecha_descarga debe indicar UTC")
        for biblioteca in ("osmnx", "networkx"):
            if not isinstance(metadatos.get(biblioteca), str) or not metadatos[biblioteca]:
                raise ValueError(f"falta la versión de {biblioteca}")
        if not isinstance(metadatos.get("uso_cache_http"), bool):
            raise ValueError("falta uso_cache_http")
    except (ValueError, KeyError, TypeError) as error:
        raise ValueError(
            f"Los metadatos de {archivo} no son válidos: {error}. "
            "Conserve o mueva la copia antes de solicitar una nueva descarga."
        ) from error
    return grafo


def asignar_pesos(G: nx.MultiDiGraph) -> nx.MultiDiGraph:
    """R1.2: devuelve una copia con velocidades, procedencia y tiempos positivos.

    Normaliza límites múltiples por mínimo e imputa con OSMnx por promedio del tipo
    de vía. El respaldo es el promedio entre tipos con datos, no un límite legal ni
    una velocidad observada. No modifica la entrada ni persiste archivos.
    """
    _validar_grafo(G)
    grafo = G.copy()
    velocidades = {}
    tipos = {}
    tipos_con_datos = set()
    for u, v, clave, datos in grafo.edges(keys=True, data=True):
        arista = (u, v, clave)
        longitud = _positivo_finito(datos.get("length"))
        if longitud is None:
            raise ValueError(f"La arista {arista} tiene longitud ausente o inválida")
        tipo = datos.get("highway")
        if isinstance(tipo, list):
            tipo = tipo[0] if tipo else None
        if not isinstance(tipo, str) or not tipo.strip():
            raise ValueError(f"La arista {arista} no tiene highway válido")
        velocidad = _normalizar_maxspeed(datos.get("maxspeed"))
        velocidades[arista] = velocidad
        tipos[arista] = tipo
        if velocidad is not None:
            tipos_con_datos.add(tipo)
        datos["longitud_m"] = longitud
        # OSMnx recibe ya el mínimo convertido. Los atributos originales se restauran
        # después; no se usa su agregación por promedio para límites múltiples.
        datos["maxspeed"] = str(velocidad) if velocidad is not None else None

    if not tipos_con_datos:
        raise ValueError("La red no tiene ninguna velocidad OSM válida para imputar")
    grafo = ox.routing.add_edge_speeds(grafo)
    for u, v, clave, datos in grafo.edges(keys=True, data=True):
        arista = (u, v, clave)
        velocidad = _positivo_finito(datos.pop("speed_kph"))
        if velocidad is None:
            raise ValueError(f"La arista {arista} no tiene velocidad finita positiva")
        tiempo_s = (datos["longitud_m"] / velocidad) * 3.6
        if _positivo_finito(tiempo_s) is None:
            raise ValueError(f"La arista {arista} no tiene tiempo finito positivo")
        datos["velocidad_kmh"] = velocidad
        datos["tiempo_s"] = tiempo_s
        if velocidades[arista] is not None:
            datos["fuente_velocidad"] = "osm"
        elif tipos[arista] in tipos_con_datos:
            datos["fuente_velocidad"] = "imputada"
        else:
            datos["fuente_velocidad"] = "defecto"
        originales = G[u][v][clave]
        if "maxspeed" in originales:
            datos["maxspeed"] = originales["maxspeed"]
        else:
            datos.pop("maxspeed", None)
    return grafo


def _positivo_finito(valor: Any) -> float | None:
    """Interpreta una magnitud física positiva; no acepta booleanos ni infinitos."""
    if isinstance(valor, bool):
        return None
    try:
        numero = float(valor)
    except (ValueError, TypeError, OverflowError):
        return None
    return numero if math.isfinite(numero) and numero > 0 else None


def _normalizar_maxspeed(valor: Any) -> float | None:
    """Convierte límites OSM a km/h; usa el mínimo solo si todos se interpretan."""
    if isinstance(valor, list):
        valores = [_normalizar_maxspeed(elemento) for elemento in valor]
        if not valores or any(elemento is None for elemento in valores):
            return None
        return min(valores)
    if isinstance(valor, str):
        if ";" in valor:
            return _normalizar_maxspeed(valor.split(";"))
        coincidencia = re.fullmatch(
            r"\s*(\d+(?:\.\d+)?)\s*(mph|km/h|kph)?\s*", valor, re.IGNORECASE,
        )
        if coincidencia is None:
            return None
        numero = float(coincidencia[1])
        if (coincidencia[2] or "").lower() == "mph":
            numero *= 1.609344
        return _positivo_finito(numero)
    return _positivo_finito(valor)


def procesar() -> None:
    """R1.2, R1.3 y R1.5: pesos -> ox.truncate.largest_component(G, strongly=True) ->
    reindexar 0..V-1 + UTM -> ``exportar_red`` y ``exportar_lugares_utm``."""
    archivo = DATA_RAW / "quindio.graphml"
    if not archivo.is_file():
        raise FileNotFoundError(f"Falta {archivo}; ejecute primero el comando descargar")
    original = _validar_guardada(archivo)
    for nodo, datos in original.nodes(data=True):
        try:
            lat, lon = float(datos["y"]), float(datos["x"])
        except (KeyError, TypeError, ValueError) as error:
            raise ValueError(f"El nodo {nodo} no tiene coordenadas válidas") from error
        if not math.isfinite(lat) or not math.isfinite(lon) or not (
            -90 <= lat <= 90 and -180 <= lon <= 180
        ):
            raise ValueError(f"El nodo {nodo} no tiene coordenadas válidas")

    ponderada = asignar_pesos(original)
    componente = ox.truncate.largest_component(ponderada, strongly=True)
    identificadores = sorted(componente.nodes)
    indices = {osmid: idx for idx, osmid in enumerate(identificadores)}
    lats = [float(componente.nodes[osmid]["y"]) for osmid in identificadores]
    lons = [float(componente.nodes[osmid]["x"]) for osmid in identificadores]
    xs, ys = a_utm_lote(lats, lons)
    if not all(math.isfinite(valor) for valor in xs + ys):
        raise ValueError("Las coordenadas UTM de la componente no son finitas")
    nodos = pd.DataFrame({
        "idx": list(range(len(identificadores))), "osmid": identificadores,
        "lat": lats, "lon": lons, "x_utm": xs, "y_utm": ys,
    })
    filas = []
    for u, v, clave, datos in sorted(componente.edges(keys=True, data=True), key=lambda a: a[:3]):
        tipo = datos["highway"]
        filas.append({
            "u": indices[u], "v": indices[v], "longitud_m": datos["longitud_m"],
            "velocidad_kmh": datos["velocidad_kmh"],
            "fuente_velocidad": datos["fuente_velocidad"], "tiempo_s": datos["tiempo_s"],
            "highway": "|".join(tipo) if isinstance(tipo, list) else tipo,
        })
    aristas = pd.DataFrame(filas, columns=COLUMNAS_ARISTAS)
    metadatos = json.loads(original.graph["metadatos_descarga"])
    metadatos.update({
        "nodos_componente": componente.number_of_nodes(),
        "aristas_componente": componente.number_of_edges(),
        "porcentaje_velocidad_osm": (
            100.0 * sum(fila["fuente_velocidad"] == "osm" for fila in filas) / len(filas)
            if filas else 0.0
        ),
    })
    exportar_red(nodos, aristas, metadatos)
    exportar_lugares_utm()


def exportar_red(nodos: pd.DataFrame, aristas: pd.DataFrame, metadatos: dict[str, Any]) -> None:
    """Escribe el contrato de archivos de la red procesada (ver docstring del módulo)."""
    faltan = [c for c in COLUMNAS_NODOS if c not in nodos.columns]
    faltan += [c for c in COLUMNAS_ARISTAS if c not in aristas.columns]
    if faltan:
        raise ValueError(f"Faltan columnas: {faltan}")
    if sorted(nodos["idx"].tolist()) != list(range(len(nodos))):
        raise ValueError("nodos.idx debe ser 0..V-1 sin huecos")
    DATA_PROCESSED.mkdir(parents=True, exist_ok=True)
    nodos[COLUMNAS_NODOS].sort_values("idx").to_csv(DATA_PROCESSED / "nodos.csv.gz", index=False)
    aristas[COLUMNAS_ARISTAS].to_csv(DATA_PROCESSED / "aristas.csv.gz", index=False)
    (DATA_PROCESSED / "metadatos.json").write_text(
        json.dumps(metadatos, ensure_ascii=False, indent=2, default=str), encoding="utf-8"
    )


def exportar_lugares_utm() -> pd.DataFrame:
    """Coordenadas UTM 18N de los lugares del catálogo, para R1.4 en Java."""
    catalogo = pd.read_csv(CATALOGO, dtype={"id": str}).dropna(subset=["lat", "lon"])
    xs, ys = a_utm_lote(catalogo["lat"].tolist(), catalogo["lon"].tolist())
    salida = pd.DataFrame({"id_lugar": catalogo["id"], "x_utm": xs, "y_utm": ys})
    DATA_PROCESSED.mkdir(parents=True, exist_ok=True)
    salida.to_csv(DATA_PROCESSED / "lugares_utm.csv", index=False)
    return salida
