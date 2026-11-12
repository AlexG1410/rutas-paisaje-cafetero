"""R1.3/R1.5: componente fuerte y exportación con archivos temporales."""

import json
from pathlib import Path
from unittest.mock import Mock

import networkx as nx
import osmnx as ox
import pandas as pd
import pytest
from pyproj import Transformer

from preparacion import __main__ as cli
from preparacion import red


@pytest.fixture
def entorno(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> tuple[Path, Mock]:
    """SCC mayor {10,20,30}; menor {40,50}; una unión sin retorno."""
    grafo = nx.MultiDiGraph(crs="epsg:4326", simplified=True)
    for nodo in [50, 30, 10, 40, 20]:
        grafo.add_node(nodo, x=-75.67, y=4.53)
    for u, v in [(10, 20), (20, 30), (30, 10), (20, 10),
                 (40, 50), (50, 40), (30, 40)]:
        grafo.add_edge(u, v, length=1000.0, highway="residential", maxspeed="60")
    grafo.add_edge(10, 20, length=2000.0, highway=["residential", "tertiary"])
    raw = tmp_path / "raw"
    raw.mkdir()
    procesada = tmp_path / "processed"
    catalogo = tmp_path / "lugares.csv"
    catalogo.write_text("id,lat,lon\nuno,4.53,-75.67\ndos,,\n", encoding="utf-8")
    monkeypatch.setattr(red, "DATA_RAW", raw)
    monkeypatch.setattr(red, "DATA_PROCESSED", procesada)
    monkeypatch.setattr(red, "CATALOGO", catalogo)
    peticion = Mock(return_value=grafo)
    monkeypatch.setattr(ox, "graph_from_place", peticion)
    red.descargar()
    peticion.reset_mock()
    peticion.side_effect = AssertionError("Procesar no debe descargar")
    return procesada, peticion


def test_tpa_b_c_componente_pesos_proyeccion(entorno: tuple[Path, Mock]) -> None:
    salida, peticion = entorno
    red.procesar()
    peticion.assert_not_called()
    nodos = pd.read_csv(salida / "nodos.csv.gz")
    aristas = pd.read_csv(salida / "aristas.csv.gz")
    metadatos = json.loads((salida / "metadatos.json").read_text(encoding="utf-8"))
    assert list(nodos.columns) == red.COLUMNAS_NODOS
    assert list(aristas.columns) == red.COLUMNAS_ARISTAS
    assert nodos["osmid"].tolist() == [10, 20, 30]
    assert nodos["idx"].tolist() == [0, 1, 2]
    assert len(aristas) == 5
    assert len(aristas[(aristas.u == 0) & (aristas.v == 1)]) == 2
    assert ((aristas.u == 1) & (aristas.v == 0)).any()
    assert set(aristas.u) | set(aristas.v) == {0, 1, 2}
    assert (aristas.tiempo_s > 0).all()
    assert aristas.tiempo_s.tolist() == pytest.approx([60, 120, 60, 60, 60])
    assert "residential|tertiary" in aristas.highway.tolist()
    assert metadatos["nodos_descargada"] == 5
    assert metadatos["aristas_descargada"] == 8
    assert metadatos["nodos_componente"] == 3
    assert metadatos["aristas_componente"] == 5
    assert metadatos["porcentaje_velocidad_osm"] == pytest.approx(80)
    original = json.loads(ox.load_graphml(red.DATA_RAW / "quindio.graphml")
                          .graph["metadatos_descarga"])
    for campo, valor in original.items():
        assert metadatos[campo] == valor
    x, y = Transformer.from_crs(4326, 32618, always_xy=True).transform(-75.67, 4.53)
    assert nodos.x_utm.tolist() == pytest.approx([x] * 3)
    assert nodos.y_utm.tolist() == pytest.approx([y] * 3)
    lugares = pd.read_csv(salida / "lugares_utm.csv")
    assert lugares.id_lugar.tolist() == ["uno"]
    assert lugares.x_utm.tolist() == pytest.approx([x])


@pytest.mark.parametrize("existe", [False, True])
def test_tpd_no_descarga_archivo_ausente_o_invalido(
    entorno: tuple[Path, Mock], existe: bool,
) -> None:
    salida, peticion = entorno
    archivo = red.DATA_RAW / "quindio.graphml"
    if existe:
        archivo.write_text("inválido", encoding="utf-8")
    else:
        archivo.unlink()
    with pytest.raises((ValueError, FileNotFoundError)):
        red.procesar()
    peticion.assert_not_called()
    assert not salida.exists()


def test_tpe_catalogo_vacio_avisa_cli(
    entorno: tuple[Path, Mock], monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture,
) -> None:
    salida, _ = entorno
    red.CATALOGO.write_text("id,lat,lon\nuno,,\n", encoding="utf-8")
    monkeypatch.setattr("sys.argv", ["preparacion", "procesar"])
    cli.main()
    assert "sin coordenadas" in capsys.readouterr().out
    assert pd.read_csv(salida / "lugares_utm.csv").empty


def test_tpf_red_conexa_y_repeticion(entorno: tuple[Path, Mock]) -> None:
    salida, _ = entorno
    archivo = red.DATA_RAW / "quindio.graphml"
    grafo = ox.load_graphml(archivo)
    grafo.add_edge(40, 10, length=500.0, highway="residential", maxspeed="30")
    meta = json.loads(grafo.graph["metadatos_descarga"])
    meta["aristas_descargada"] = grafo.number_of_edges()
    grafo.graph["metadatos_descarga"] = json.dumps(meta)
    ox.save_graphml(grafo, archivo)
    antes = archivo.read_bytes()
    red.procesar()
    nodos = pd.read_csv(salida / "nodos.csv.gz")
    aristas = pd.read_csv(salida / "aristas.csv.gz")
    meta_antes = (salida / "metadatos.json").read_text(encoding="utf-8")
    red.procesar()
    assert len(nodos) == 5
    pd.testing.assert_frame_equal(nodos, pd.read_csv(salida / "nodos.csv.gz"))
    pd.testing.assert_frame_equal(aristas, pd.read_csv(salida / "aristas.csv.gz"))
    assert meta_antes == (salida / "metadatos.json").read_text(encoding="utf-8")
    assert antes == archivo.read_bytes()


@pytest.mark.parametrize("valor", [None, float("inf"), float("nan"), 200])
def test_tpg_coordenadas_invalidas(entorno: tuple[Path, Mock], valor: object) -> None:
    salida, _ = entorno
    archivo = red.DATA_RAW / "quindio.graphml"
    grafo = ox.load_graphml(archivo)
    if valor is None:
        del grafo.nodes[10]["x"]
    else:
        grafo.nodes[10]["x"] = valor
    ox.save_graphml(grafo, archivo)
    with pytest.raises(ValueError, match="coordenadas"):
        red.procesar()
    assert not salida.exists()
