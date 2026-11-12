"""Figuras R1 desde CSV sintéticos, sin medir ni modificar resultados reales."""

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd
import pytest

from preparacion import graficas


@pytest.fixture
def entorno(tmp_path, monkeypatch):
    origen = tmp_path / "results" / "r1"
    origen.mkdir(parents=True)
    destino = tmp_path / "figuras"
    monkeypatch.setattr(graficas, "RESULTS", origen.parent)
    monkeypatch.setattr(graficas, "FIGURAS", destino)
    return origen / "tiempo_componente.csv", destino / "r1_tiempo_componente.png"


def test_t1fa_figura_desde_csv_sin_cambiar_entrada(entorno):
    origen, destino = entorno
    pd.DataFrame({"red": ["mayor", "menor"], "V_mas_E": [20, 10],
                  "tiempo_ms": [2.5, 1.5], "estado": ["completada"] * 2}).to_csv(origen, index=False)
    contenido = origen.read_bytes()
    graficas.ejecutar()
    assert destino.read_bytes().startswith(b"\x89PNG")
    assert origen.read_bytes() == contenido
    assert not plt.get_fignums()


def test_t1fb_timeout_marcado_sin_tiempo_ficticio(entorno, monkeypatch):
    origen, destino = entorno
    pd.DataFrame({"red": ["breve", "larga"], "V_mas_E": [10, 20],
                  "tiempo_ms": [1.5, None],
                  "estado": ["completada", "no completada"]}).to_csv(origen, index=False)
    from matplotlib.axes import Axes
    original = Axes.annotate
    textos = []

    def registrar(self, texto, *args, **kwargs):
        textos.append(texto)
        return original(self, texto, *args, **kwargs)

    monkeypatch.setattr(Axes, "annotate", registrar)
    graficas.ejecutar()
    assert destino.exists()
    assert any("larga" in texto and "no completada" in texto for texto in textos)


@pytest.mark.parametrize("contenido", [
    "red,V_mas_E,tiempo_ms,estado\n",
    "red,V_mas_E\na,10\n",
    "red,V_mas_E,tiempo_ms,estado\na,10,nan,completada\n",
    "red,V_mas_E,tiempo_ms,estado\na,inf,1,completada\n",
    "red,V_mas_E,tiempo_ms,estado\na,10,1,desconocido\n",
])
def test_t1fc_datos_invalidos_no_generan(entorno, contenido):
    origen, destino = entorno
    origen.write_text(contenido, encoding="utf-8")
    with pytest.raises(ValueError):
        graficas.ejecutar()
    assert not destino.exists()


def test_t2fa_figura_dijkstra_dos_versiones_y_timeout(entorno, monkeypatch):
    origen_r1, _ = entorno
    carpeta = origen_r1.parent.parent / "r2"
    carpeta.mkdir()
    origen = carpeta / "mediciones_dijkstra.csv"
    pd.DataFrame({"origen": ["a", "a", "b"], "destino": ["b", "b", "c"],
                  "version": ["lineal", "cola_prioridad", "lineal"],
                  "nodos_explorados": [30, 30, None], "tiempo_ms": [4, 1, None],
                  "estado": ["completada", "completada", "no completada"]}).to_csv(origen, index=False)
    contenido = origen.read_bytes()
    from matplotlib.axes import Axes
    original = Axes.text
    textos = []

    def registrar(self, x, y, texto, *args, **kwargs):
        textos.append(texto)
        return original(self, x, y, texto, *args, **kwargs)

    monkeypatch.setattr(Axes, "text", registrar)
    graficas.ejecutar()
    assert (graficas.FIGURAS / "r2_tiempo_vs_explorados.png").read_bytes().startswith(b"\x89PNG")
    assert origen.read_bytes() == contenido
    assert any("no completada" in texto for texto in textos)
    assert not plt.get_fignums()


@pytest.mark.parametrize("columna,valor", [("tiempo_ms", float("nan")),
                                         ("nodos_explorados", 0.5),
                                         ("version", "otra"), ("estado", "otra")])
def test_t2fa_figura_rechaza_datos_invalidos(entorno, columna, valor):
    origen_r1, _ = entorno
    carpeta = origen_r1.parent.parent / "r2"
    carpeta.mkdir()
    fila = {"origen": "a", "destino": "b", "version": "lineal",
            "nodos_explorados": 30, "tiempo_ms": 4, "estado": "completada"}
    fila[columna] = valor
    pd.DataFrame([fila]).to_csv(carpeta / "mediciones_dijkstra.csv", index=False)
    with pytest.raises(ValueError):
        graficas.ejecutar()
    assert not (graficas.FIGURAS / "r2_tiempo_vs_explorados.png").exists()
