"""R1: descarga sin red y persistencia GraphML real con OSMnx."""

from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path
from unittest.mock import Mock

import networkx as nx
import osmnx as ox
import pytest

from preparacion import __main__ as cli
from preparacion import red


@pytest.fixture
def entorno(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> tuple[Path, Mock]:
    """Grafo pequeño: dos componentes, sentidos distintos y una arista paralela."""
    grafo = nx.MultiDiGraph(crs="epsg:4326", simplified=True)
    grafo.add_node(10, x=-75.67, y=4.53)
    grafo.add_node(20, x=-75.66, y=4.54)
    grafo.add_node(30, x=-75.65, y=4.55)
    grafo.add_node(40, x=-75.64, y=4.56)
    grafo.add_edge(10, 20, length=100.0, osmid=1001)
    grafo.add_edge(10, 20, length=120.0, osmid=1002)
    grafo.add_edge(20, 10, length=110.0, osmid=1003)
    grafo.add_edge(30, 40, length=90.0, osmid=1004)
    peticion = Mock(return_value=grafo)
    monkeypatch.setattr(ox, "graph_from_place", peticion)
    monkeypatch.setattr(red, "DATA_RAW", tmp_path, raising=False)
    return tmp_path / "quindio.graphml", peticion


def test_t1da_descarga_y_graphml_con_metadatos(entorno: tuple[Path, Mock]) -> None:
    archivo, peticion = entorno
    red.descargar()

    peticion.assert_called_once_with(
        "Quindío, Colombia", network_type="drive", retain_all=True, simplify=True,
    )
    guardado = ox.load_graphml(archivo)
    assert isinstance(guardado, nx.MultiDiGraph)
    assert set(guardado.nodes) == {10, 20, 30, 40}
    assert guardado.number_of_edges() == 4
    assert len(guardado[10][20]) == 2
    assert guardado[10][20][0]["length"] == 100.0
    metadatos = json.loads(guardado.graph["metadatos_descarga"])
    assert metadatos["consulta"] == "Quindío, Colombia"
    assert metadatos["network_type"] == "drive"
    assert metadatos["retain_all"] is True
    assert metadatos["simplify"] is True
    assert metadatos["osmnx"] == ox.__version__
    assert metadatos["networkx"] == nx.__version__
    assert metadatos["nodos_descargada"] == 4
    assert metadatos["aristas_descargada"] == 4
    assert isinstance(metadatos["uso_cache_http"], bool)
    assert datetime.fromisoformat(metadatos["fecha_descarga"]).utcoffset().total_seconds() == 0


def test_t1db_reutiliza_sin_red_ni_reescritura(entorno: tuple[Path, Mock]) -> None:
    archivo, peticion = entorno
    red.descargar()
    contenido = archivo.read_bytes()
    modificacion = archivo.stat().st_mtime_ns
    peticion.reset_mock()
    peticion.side_effect = AssertionError("No debe volver a descargar")

    red.descargar()

    peticion.assert_not_called()
    assert archivo.read_bytes() == contenido
    assert archivo.stat().st_mtime_ns == modificacion


def test_t1dc_archivo_ilegible_se_conserva(entorno: tuple[Path, Mock]) -> None:
    archivo, peticion = entorno
    archivo.write_text("no es GraphML", encoding="utf-8")
    with pytest.raises(ValueError, match="No se puede reutilizar"):
        red.descargar()
    assert archivo.read_text(encoding="utf-8") == "no es GraphML"
    peticion.assert_not_called()


def test_t1dd_no_inventa_metadatos(entorno: tuple[Path, Mock]) -> None:
    archivo, peticion = entorno
    ox.save_graphml(peticion.return_value, archivo)
    contenido = archivo.read_bytes()
    with pytest.raises(ValueError, match="metadatos"):
        red.descargar()
    assert archivo.read_bytes() == contenido
    peticion.assert_not_called()


@pytest.mark.parametrize(
    ("campo", "valor"),
    [("retain_all", False), ("consulta", "Otro lugar"), ("network_type", "walk"),
     ("simplify", False), ("nodos_descargada", 99), ("aristas_descargada", 99),
     ("fecha_descarga", "fecha inválida")],
)
def test_t1de_rechaza_metadatos_incompatibles(
    entorno: tuple[Path, Mock], campo: str, valor: object,
) -> None:
    archivo, peticion = entorno
    red.descargar()
    grafo = ox.load_graphml(archivo)
    metadatos = json.loads(grafo.graph["metadatos_descarga"])
    metadatos[campo] = valor
    grafo.graph["metadatos_descarga"] = json.dumps(metadatos)
    ox.save_graphml(grafo, archivo)
    contenido = archivo.read_bytes()
    peticion.reset_mock()

    with pytest.raises(ValueError, match="metadatos"):
        red.descargar()

    assert archivo.read_bytes() == contenido
    peticion.assert_not_called()


def test_t1df_rechaza_red_vacia(entorno: tuple[Path, Mock]) -> None:
    archivo, peticion = entorno
    peticion.return_value = nx.MultiDiGraph()
    with pytest.raises(ValueError, match="vacía"):
        red.descargar()
    assert not archivo.exists()


def test_t1dg_error_de_conexion_desde_cli(
    entorno: tuple[Path, Mock], monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture,
) -> None:
    archivo, peticion = entorno
    peticion.side_effect = OSError("sin conexión")
    monkeypatch.setattr("sys.argv", ["preparacion", "descargar"])
    with pytest.raises(SystemExit) as salida:
        cli.main()
    assert salida.value.code == 1
    assert "sin conexión" in capsys.readouterr().err
    assert not archivo.exists()


def test_t1dh_no_deja_archivo_parcial(
    entorno: tuple[Path, Mock], monkeypatch: pytest.MonkeyPatch,
) -> None:
    archivo, _ = entorno

    def falla_guardado(grafo: nx.MultiDiGraph, filepath: Path) -> None:
        Path(filepath).write_text("parcial", encoding="utf-8")
        raise OSError("fallo de escritura")

    monkeypatch.setattr(ox, "save_graphml", falla_guardado)
    with pytest.raises(OSError, match="fallo de escritura"):
        red.descargar()
    assert not archivo.exists()
    assert list(archivo.parent.iterdir()) == []


def test_t1di_cli_anuncia_archivo_disponible(
    entorno: tuple[Path, Mock], monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture,
) -> None:
    archivo, _ = entorno
    monkeypatch.setattr("sys.argv", ["preparacion", "descargar"])
    cli.main()
    salida = capsys.readouterr().out
    assert "Preparando" in salida
    assert str(archivo) in salida
    assert archivo.exists()
