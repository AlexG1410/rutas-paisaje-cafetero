"""R1.2: pesos y procedencia con OSMnx real, sin peticiones de red."""

from copy import deepcopy

import networkx as nx
import pytest

from preparacion.red import asignar_pesos


def red_prueba(filas: list[dict]) -> nx.MultiDiGraph:
    """Construye vías paralelas con coordenadas requeridas por OSMnx."""
    grafo = nx.MultiDiGraph(crs="epsg:4326")
    grafo.add_node(1, x=-75.6, y=4.5)
    grafo.add_node(2, x=-75.7, y=4.6)
    for fila in filas:
        grafo.add_edge(1, 2, **{"length": 1000.0, "highway": "residential", **fila})
    return grafo


def test_t12a_tiempo_y_copia() -> None:
    original = red_prueba([{"maxspeed": "60"}])
    antes = deepcopy(original)
    resultado = asignar_pesos(original)
    datos = resultado[1][2][0]
    assert datos["tiempo_s"] == pytest.approx(60)
    assert datos["longitud_m"] == 1000
    assert datos["fuente_velocidad"] == "osm"
    assert resultado is not original
    assert nx.utils.graphs_equal(original, antes)


@pytest.mark.parametrize(("valor", "esperado"), [
    (50, 50), (" 50 ", 50), ("30 mph", 48.28032), ("40 km/h", 40),
    ("40 kph", 40), (["60", "30"], 30), (["30 mph", "50"], 48.28032),
    ("50;30", 30),
])
def test_t12b_conversion_y_minimo(valor: object, esperado: float) -> None:
    resultado = asignar_pesos(red_prueba([{"maxspeed": valor}]))
    assert resultado[1][2][0]["velocidad_kmh"] == pytest.approx(esperado)
    assert resultado[1][2][0]["maxspeed"] == valor


def test_t12c_d_promedios_y_respaldo() -> None:
    resultado = asignar_pesos(red_prueba([
        {"maxspeed": "30"}, {"maxspeed": "50"}, {},
        {"highway": "primary", "maxspeed": "80"}, {"highway": "service"},
    ]))
    assert resultado[1][2][2]["velocidad_kmh"] == pytest.approx(40)
    assert resultado[1][2][2]["fuente_velocidad"] == "imputada"
    assert resultado[1][2][4]["velocidad_kmh"] == pytest.approx(60)
    assert resultado[1][2][4]["fuente_velocidad"] == "defecto"


@pytest.mark.parametrize("valor", [None, "signals", "none", "", "0", "-10",
                                  "nan", "inf", float("inf"), [],
                                  ["30", "signals"], "30 knots", True])
def test_t12e_invalida_se_imputa(valor: object) -> None:
    resultado = asignar_pesos(red_prueba([{"maxspeed": "40"}, {"maxspeed": valor}]))
    assert resultado[1][2][1]["velocidad_kmh"] == pytest.approx(40)
    assert resultado[1][2][1]["fuente_velocidad"] == "imputada"


def test_t12f_sin_velocidades_aborta() -> None:
    with pytest.raises(ValueError, match="velocidad OSM válida"):
        asignar_pesos(red_prueba([{}]))


@pytest.mark.parametrize("valor", [None, "texto", 0, -1, float("nan"), float("inf"), True])
def test_t12g_longitudes_invalidas(valor: object) -> None:
    original = red_prueba([{"maxspeed": "60", "length": valor}])
    with pytest.raises(ValueError, match="longitud"):
        asignar_pesos(original)
    assert "tiempo_s" not in original[1][2][0]


def test_t12g_longitud_ausente() -> None:
    original = red_prueba([{"maxspeed": "60"}])
    del original[1][2][0]["length"]
    with pytest.raises(ValueError, match="longitud"):
        asignar_pesos(original)


def test_t12h_i_tipo_multiple_y_estructura() -> None:
    original = red_prueba([
        {"maxspeed": "30"},
        {"highway": ["residential", "primary"]},
        {"highway": "primary", "maxspeed": "80"},
    ])
    resultado = asignar_pesos(original)
    assert resultado[1][2][1]["velocidad_kmh"] == 30
    assert resultado[1][2][1]["highway"] == ["residential", "primary"]
    assert list(resultado.edges(keys=True)) == list(original.edges(keys=True))
    for _, _, datos in resultado.edges(data=True):
        assert datos["tiempo_s"] > 0
        assert "speed_kph" not in datos


@pytest.mark.parametrize("tipo", [None, [], ""])
def test_t12h_tipo_invalido(tipo: object) -> None:
    with pytest.raises(ValueError, match="highway"):
        asignar_pesos(red_prueba([{"highway": tipo, "maxspeed": "30"}]))
