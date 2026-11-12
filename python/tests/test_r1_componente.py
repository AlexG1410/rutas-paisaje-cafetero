"""R1.3 experimental: archivos reales temporales, sin HTTP ni mediciones reales."""

import json
from unittest.mock import Mock

import networkx as nx
import osmnx as ox
import pandas as pd
import pytest

from preparacion import r1_componente as r1
from preparacion.medicion import Medicion


def _grafo():
    grafo = nx.MultiDiGraph(crs="epsg:4326", simplified=True)
    for nodo in range(5):
        grafo.add_node(nodo, x=-75.67, y=4.53)
    grafo.add_edges_from([(0, 1), (1, 0), (0, 1), (1, 2), (3, 4), (4, 3)])
    return grafo


def test_t1ea_descarga_reutiliza_y_preserva(tmp_path, monkeypatch):
    peticion = Mock(return_value=_grafo())
    monkeypatch.setattr(ox, "graph_from_point", peticion)
    ruta = tmp_path / "red.graphml"
    r1.preparar_red(ruta, (4.53, -75.67), 2)
    peticion.assert_called_once_with((4.53, -75.67), dist=2000, dist_type="bbox",
                                    network_type="drive", retain_all=True, simplify=True)
    contenido = ruta.read_bytes()
    fecha = ruta.stat().st_mtime_ns
    peticion.reset_mock()
    recuperada = r1.preparar_red(ruta, (4.53, -75.67), 2)
    peticion.assert_not_called()
    assert recuperada.number_of_edges() == 6
    assert ruta.read_bytes() == contenido and ruta.stat().st_mtime_ns == fecha
    meta = json.loads(recuperada.graph["metadatos_descarga"])
    assert meta["dist_type"] == "bbox" and meta["radio_km"] == 2


@pytest.mark.parametrize("corrupcion", ["xml", "centro", "conteo", "metadatos"])
def test_t1eb_copia_invalida_no_descarga(tmp_path, monkeypatch, corrupcion):
    peticion = Mock(return_value=_grafo())
    monkeypatch.setattr(ox, "graph_from_point", peticion)
    ruta = tmp_path / "red.graphml"
    r1.preparar_red(ruta, (4.53, -75.67), 2)
    if corrupcion == "xml":
        ruta.write_text("invalido", encoding="utf-8")
    else:
        grafo = ox.load_graphml(ruta)
        meta = json.loads(grafo.graph["metadatos_descarga"])
        if corrupcion == "metadatos":
            meta = []
        else:
            meta["centro" if corrupcion == "centro" else "nodos_descargada"] = None
        grafo.graph["metadatos_descarga"] = json.dumps(meta)
        ox.save_graphml(grafo, filepath=ruta)
    contenido = ruta.read_bytes()
    peticion.reset_mock()
    with pytest.raises(ValueError):
        r1.preparar_red(ruta, (4.53, -75.67), 2)
    peticion.assert_not_called()
    assert ruta.read_bytes() == contenido


@pytest.mark.parametrize("filas", ["otro,4,-75", "cab_armenia,nan,-75",
                                    "cab_armenia,91,-75", "cab_armenia,4,-75\ncab_armenia,4,-75"])
def test_t1eb_centro_invalido(tmp_path, filas):
    ruta = tmp_path / "catalogo.csv"
    ruta.write_text("id,lat,lon\n" + filas, encoding="utf-8")
    with pytest.raises(ValueError):
        r1.leer_centro(ruta)


def test_t1ec_verifica_empate_y_aristas_paralelas():
    grafo = _grafo()
    componente = r1.verificar(grafo)
    assert componente.number_of_nodes() == 2
    r1.validar_componente(grafo, grafo.subgraph([3, 4]).copy())
    mala = grafo.subgraph([0, 1]).copy()
    mala.remove_edge(0, 1)
    with pytest.raises(ValueError):
        r1.validar_componente(grafo, mala)
    with pytest.raises(ValueError):
        r1.validar_componente(grafo, grafo.subgraph([2]).copy())


@pytest.fixture
def entorno(tmp_path, monkeypatch):
    raw = tmp_path / "raw"
    raw.mkdir()
    catalogo = tmp_path / "catalogo.csv"
    catalogo.write_text("id,lat,lon\ncab_armenia,4.53,-75.67\n", encoding="utf-8")
    monkeypatch.setattr(r1, "DATA_RAW", raw)
    monkeypatch.setattr(r1, "RESULTS", tmp_path / "results")
    monkeypatch.setattr(r1, "CATALOGO", catalogo)
    monkeypatch.setattr(r1.red, "_validar_guardada", lambda ruta: _grafo())
    monkeypatch.setattr(ox, "graph_from_point", Mock(side_effect=lambda *a, **k: _grafo()))
    return tmp_path / "results"


def test_t1ed_serie_y_timeout(entorno, monkeypatch):
    llamadas = []

    def medir(fn, grafo, **kwargs):
        assert fn is ox.truncate.largest_component and kwargs == {"strongly": True}
        llamadas.append(grafo)
        if len(llamadas) == 2:
            return Medicion([], False, None)
        return Medicion([1, 2, 3, 4, 5], True, fn(grafo, **kwargs))

    monkeypatch.setattr(r1, "medir_con_limite", medir)
    r1.ejecutar()
    tiempos = pd.read_csv(entorno / "r1/tiempo_componente.csv")
    tamanos = pd.read_csv(entorno / "r1/tamano_red.csv")
    assert len(llamadas) == 5 and len(tiempos) == 5 and len(tamanos) == 10
    assert list(tiempos.columns) == ["red", "V", "E", "V_mas_E", "tiempo_ms", "repeticiones", "estado"]
    assert tiempos.iloc[1]["estado"] == "no completada"
    assert pd.isna(tiempos.iloc[1]["tiempo_ms"])
    assert set(tiempos["V_mas_E"]) == {11}
    assert all(grafo.number_of_edges() == 6 for grafo in llamadas)
    assert (entorno / "maquina_python.csv").exists()


def test_t1ee_fallo_verificacion_no_mide(entorno, monkeypatch):
    medidor = Mock()
    monkeypatch.setattr(r1, "medir_con_limite", medidor)
    monkeypatch.setattr(r1, "verificar", Mock(side_effect=ValueError("SCC incorrecta")))
    with pytest.raises(ValueError, match="SCC incorrecta"):
        r1.ejecutar()
    medidor.assert_not_called()
    assert not (entorno / "r1/tiempo_componente.csv").exists()


def test_t1ee_resultado_medido_incorrecto_no_sobrescribe_tablas(entorno, monkeypatch):
    salida = entorno / "r1"
    salida.mkdir(parents=True)
    archivo = salida / "tiempo_componente.csv"
    archivo.write_text("resultado anterior", encoding="utf-8")
    monkeypatch.setattr(r1, "medir_con_limite", Mock(return_value=Medicion(
        [1], True, _grafo().subgraph([2]).copy(),
    )))
    with pytest.raises(ValueError, match="tamaño máximo"):
        r1.ejecutar()
    assert archivo.read_text(encoding="utf-8") == "resultado anterior"
    assert not (salida / "tamano_red.csv").exists()
