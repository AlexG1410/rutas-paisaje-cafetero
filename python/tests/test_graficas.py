"""Figuras R1/R2 y análisis derivado desde CSV sintéticos, sin medir."""

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
    pd.DataFrame({"red": ["grafo_java", "grafo_java"], "V": [100, 100]}).to_csv(
        origen / "memoria_representacion.csv", index=False)
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
    from matplotlib.figure import Figure
    guardar = Figure.savefig

    def comprobar_figura(self, *args, **kwargs):
        eje = self.axes[0]
        assert eje.get_yscale() == "log"
        assert eje.get_xlabel() == "Nodos explorados (cantidad)"
        assert len(eje.collections) == 2
        return guardar(self, *args, **kwargs)

    monkeypatch.setattr(Figure, "savefig", comprobar_figura)
    graficas.ejecutar()
    assert (graficas.FIGURAS / "r2_tiempo_vs_explorados.png").read_bytes().startswith(b"\x89PNG")
    assert origen.read_bytes() == contenido
    assert any("no completada" in texto for texto in textos)
    assert not plt.get_fignums()
    normalizacion = pd.read_csv(carpeta / "normalizacion_lineal.csv")
    assert len(normalizacion) == 2
    assert normalizacion.V.tolist() == [100, 100]
    assert normalizacion.ns_por_posicion.iloc[0] == pytest.approx(4e6 / (30 * 100))
    assert normalizacion.loc[1, ["nodos_explorados", "tiempo_ms", "ns_por_posicion"]].isna().all()


@pytest.mark.parametrize("columna,valor", [("tiempo_ms", float("nan")),
                                         ("tiempo_ms", 0),
                                         ("nodos_explorados", 101),
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
    assert not (carpeta / "normalizacion_lineal.csv").exists()


@pytest.mark.parametrize("valores", [None, [], [0], [1.5], [float("inf")], [100, 101]])
def test_t2nb_tamano_invalido_no_sobrescribe_salidas(entorno, valores):
    origen_r1, _ = entorno
    memoria = origen_r1.parent / "memoria_representacion.csv"
    if valores is None:
        memoria.unlink()
    else:
        pd.DataFrame({"red": ["grafo_java"] * len(valores), "V": valores}).to_csv(memoria, index=False)
    carpeta = origen_r1.parent.parent / "r2"
    carpeta.mkdir()
    pd.DataFrame({"origen": ["a"], "destino": ["b"], "version": ["lineal"],
                  "nodos_explorados": [30], "tiempo_ms": [4], "estado": ["completada"]}).to_csv(
        carpeta / "mediciones_dijkstra.csv", index=False)
    salida = carpeta / "normalizacion_lineal.csv"
    salida.write_text("anterior", encoding="utf-8")
    figura = graficas.FIGURAS / "r2_tiempo_vs_explorados.png"
    figura.parent.mkdir(parents=True)
    figura.write_bytes(b"anterior")
    with pytest.raises((ValueError, FileNotFoundError)):
        graficas.ejecutar()
    assert salida.read_text(encoding="utf-8") == "anterior"
    assert figura.read_bytes() == b"anterior"


def test_t2fb_solo_timeout_sin_tiempos_logaritmicos_ficticios(entorno):
    origen_r1, _ = entorno
    carpeta = origen_r1.parent.parent / "r2"
    carpeta.mkdir()
    pd.DataFrame({"origen": ["a"], "destino": ["b"], "version": ["lineal"],
                  "nodos_explorados": [None], "tiempo_ms": [None],
                  "estado": ["no completada"]}).to_csv(carpeta / "mediciones_dijkstra.csv", index=False)
    graficas.ejecutar()
    tabla = pd.read_csv(carpeta / "normalizacion_lineal.csv")
    assert tabla.loc[0, ["nodos_explorados", "tiempo_ms", "ns_por_posicion"]].isna().all()
    assert (graficas.FIGURAS / "r2_tiempo_vs_explorados.png").exists()
    assert not plt.get_fignums()


def test_t3fa_figura_por_conjunto_y_timeout(entorno, monkeypatch):
    origen_r1, _ = entorno
    carpeta = origen_r1.parent.parent / "r3"
    carpeta.mkdir()
    pd.DataFrame({"conjunto": ["red", "cuadrado_1000"], "n": [200, 1000], "tiempo_propio_ms": [2, 3],
                  "tiempo_scipy_ms": [1, None], "estado_propio": ["completada"] * 2,
                  "estado_scipy": ["completada", "no completada"]}).to_csv(
        carpeta / "envolvente.csv", index=False)
    from matplotlib.figure import Figure
    guardar = Figure.savefig

    def comprobar(self, *args, **kwargs):
        eje = self.axes[0]
        assert sorted(barra.get_height() for barra in eje.patches) == [1, 2, 3]
        assert any("no completada" in texto.get_text() for texto in eje.texts)
        return guardar(self, *args, **kwargs)

    monkeypatch.setattr(Figure, "savefig", comprobar)
    graficas.ejecutar()
    assert (graficas.FIGURAS / "r3_tiempos.png").exists()
    tabla = pd.read_csv(carpeta / "tiempo_por_punto.csv")
    assert tabla.propio_us_por_punto.tolist() == [10, 3]
    assert tabla.scipy_us_por_punto.iloc[0] == 5
    assert pd.isna(tabla.scipy_us_por_punto.iloc[1])
    assert not plt.get_fignums()


def test_t3fa_figura_rechaza_timeout_con_tiempo(entorno):
    origen_r1, _ = entorno
    carpeta = origen_r1.parent.parent / "r3"
    carpeta.mkdir()
    pd.DataFrame({"conjunto": ["red"], "n": [200], "tiempo_propio_ms": [2], "tiempo_scipy_ms": [1],
                  "estado_propio": ["completada"], "estado_scipy": ["no completada"]}).to_csv(
        carpeta / "envolvente.csv", index=False)
    with pytest.raises(ValueError):
        graficas.ejecutar()
    assert not (graficas.FIGURAS / "r3_tiempos.png").exists()


@pytest.mark.parametrize("n", [0, -1, 1.5, float("inf"), None])
def test_t3fb_normalizacion_rechaza_tamano_invalido(entorno, n):
    origen_r1, _ = entorno
    carpeta = origen_r1.parent.parent / "r3"
    carpeta.mkdir()
    pd.DataFrame({"conjunto": ["red"], "n": [n], "tiempo_propio_ms": [2],
                  "tiempo_scipy_ms": [1], "estado_propio": ["completada"],
                  "estado_scipy": ["completada"]}).to_csv(carpeta / "envolvente.csv", index=False)
    salida = carpeta / "tiempo_por_punto.csv"
    salida.write_text("anterior", encoding="utf-8")
    with pytest.raises(ValueError):
        graficas.graficar_envolvente()
    assert salida.read_text(encoding="utf-8") == "anterior"


def test_t4fa_figura_log_y_cocientes(entorno, monkeypatch):
    origen, _ = entorno
    carpeta = origen.parent.parent / "r4"
    carpeta.mkdir()
    pd.DataFrame({"k": [5, 6, 7], "tiempo_fuerza_bruta_ms": [5, 36, None],
                  "tiempo_held_karp_ms": [1, 3, 5],
                  "estado_fuerza_bruta": ["completada", "completada", "no completada"],
                  "estado_held_karp": ["completada"] * 3}).to_csv(carpeta / "exactas.csv", index=False)
    from matplotlib.figure import Figure
    guardar = Figure.savefig

    def comprobar(self, *args, **kwargs):
        if str(args[0]).endswith("r4_tiempo_vs_k.png"):
            assert self.axes[0].get_yscale() == "log"
            assert any("no completada" in t.get_text() for t in self.axes[0].texts)
        return guardar(self, *args, **kwargs)

    monkeypatch.setattr(Figure, "savefig", comprobar)
    graficas.ejecutar()
    tabla = pd.read_csv(carpeta / "cocientes_tiempos.csv")
    assert len(tabla) == 3
    bf = tabla[tabla.metodo.eq("fuerza_bruta")].iloc[0]
    assert bf.razon_observada == pytest.approx(7.2)
    assert bf.razon_modelo == pytest.approx(7.2)
    hk = tabla[tabla.metodo.eq("held_karp")].iloc[0]
    assert hk.razon_observada == 3
    assert hk.razon_modelo == pytest.approx(2 * (6 / 5)**2)
    assert (graficas.FIGURAS / "r4_tiempo_vs_k.png").exists()
    assert not plt.get_fignums()


@pytest.mark.parametrize("malo", [0, -1, float("inf")])
def test_t4fa_tiempo_invalido_no_publica(entorno, malo):
    origen, _ = entorno
    carpeta = origen.parent.parent / "r4"
    carpeta.mkdir()
    pd.DataFrame({"k": [5], "tiempo_fuerza_bruta_ms": [malo], "tiempo_held_karp_ms": [1],
                  "estado_fuerza_bruta": ["completada"], "estado_held_karp": ["completada"]}).to_csv(
        carpeta / "exactas.csv", index=False)
    with pytest.raises(ValueError):
        graficas.graficar_tsp()
    assert not (carpeta / "cocientes_tiempos.csv").exists()


def test_t4fa_solo_timeout_y_saltos_sin_cocientes(entorno):
    origen, _ = entorno
    carpeta = origen.parent.parent / "r4"
    carpeta.mkdir()
    pd.DataFrame({"k": [5, 7], "tiempo_fuerza_bruta_ms": [None] * 2,
                  "tiempo_held_karp_ms": [None] * 2,
                  "estado_fuerza_bruta": ["no completada"] * 2,
                  "estado_held_karp": ["no completada"] * 2}).to_csv(carpeta / "exactas.csv", index=False)
    graficas.graficar_tsp()
    assert pd.read_csv(carpeta / "cocientes_tiempos.csv").empty
    assert not plt.get_fignums()
