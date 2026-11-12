"""R3: verificación SciPy real, mediciones sustituidas y archivos temporales."""

import pandas as pd
import pytest
from scipy.spatial import ConvexHull

from preparacion import envolvente_scipy as e
from preparacion.medicion import Medicion


@pytest.fixture
def entorno(tmp_path, monkeypatch):
    carpeta = tmp_path / "r3"
    carpeta.mkdir()
    nombres = ["red", "cuadrado_1000", "cuadrado_10000", "cuadrado_50000"]
    puntos = pd.DataFrame({"x": [0, 4, 4, 0, 2], "y": [0, 0, 4, 4, 2]})
    for nombre in nombres:
        puntos.to_csv(carpeta / f"puntos_{nombre}.csv", index=False)
        puntos.iloc[:4].to_csv(carpeta / f"vertices_{nombre}.csv", index=False)
    pd.DataFrame({"conjunto": nombres, "n": [5] * 4, "vertices": [4] * 4,
                  "tiempo_propio_ms": [4] * 4, "repeticiones": [5] * 4,
                  "semilla": [2026] * 4, "estado": ["completada"] * 4}).to_csv(
        carpeta / "envolvente_propio.csv", index=False)
    monkeypatch.setattr(e, "RESULTS", tmp_path)
    llamadas = []

    def medir(fn, puntos):
        assert fn is ConvexHull
        assert puntos.dtype == "float64"
        llamadas.append(len(puntos))
        return Medicion([2.0] * 5, True, fn(puntos))

    monkeypatch.setattr(e, "medir_con_limite", medir)
    return carpeta, llamadas


def test_t3va_verifica_mide_y_guarda_tabla(entorno):
    carpeta, llamadas = entorno
    antes = (carpeta / "puntos_red.csv").read_bytes()
    e.ejecutar()
    tabla = pd.read_csv(carpeta / "envolvente.csv")
    assert llamadas == [5] * 4
    assert tabla.coinciden.all()
    assert tabla.cociente.tolist() == [2] * 4
    assert tabla.repeticiones_scipy.tolist() == [5] * 4
    assert (carpeta / "maquina_python.csv").exists()
    assert (carpeta / "puntos_red.csv").read_bytes() == antes


@pytest.mark.parametrize("problema", ["vertice", "orden", "duplicado", "n", "estado", "semilla",
                                     "colineales", "fraccion", "falta_conjunto"])
def test_t3vb_error_antes_de_medir_no_sobrescribe(entorno, problema):
    carpeta, llamadas = entorno
    salida = carpeta / "envolvente.csv"
    salida.write_text("anterior", encoding="utf-8")
    if problema in ("n", "estado", "semilla", "falta_conjunto"):
        ruta = carpeta / "envolvente_propio.csv"
        tabla = pd.read_csv(ruta)
        if problema == "falta_conjunto":
            tabla = tabla.iloc[:3]
        else:
            tabla.loc[3, problema] = {"n": 99, "estado": "otro", "semilla": 0}[problema]
    else:
        ruta = carpeta / ("puntos_cuadrado_50000.csv" if problema in
                          ("colineales", "fraccion") else "vertices_cuadrado_50000.csv")
        tabla = pd.read_csv(ruta)
        if problema == "vertice":
            tabla.loc[0, "x"] = 99
        elif problema == "orden":
            tabla = tabla.iloc[::-1]
        elif problema == "duplicado":
            tabla = pd.concat([tabla, tabla.iloc[[0]]])
        elif problema == "fraccion":
            tabla["x"] = tabla.x.astype(float)
            tabla.loc[0, "x"] = 0.5
        else:
            tabla["y"] = 0
    tabla.to_csv(ruta, index=False)
    with pytest.raises(ValueError):
        e.ejecutar()
    assert not llamadas
    assert salida.read_text(encoding="utf-8") == "anterior"


def test_t3vb_resultado_medido_incorrecto(entorno, monkeypatch):
    carpeta, _ = entorno
    monkeypatch.setattr(e, "medir_con_limite", lambda fn, puntos: Medicion([1], True, None))
    with pytest.raises(ValueError):
        e.ejecutar()
    assert not (carpeta / "envolvente.csv").exists()


def test_t3va_timeout_no_inventa_tiempos(entorno, monkeypatch):
    carpeta, _ = entorno
    monkeypatch.setattr(e, "medir_con_limite", lambda fn, puntos: Medicion([], False, None))
    e.ejecutar()
    tabla = pd.read_csv(carpeta / "envolvente.csv")
    assert tabla.coinciden.all()  # Verificación previa sí terminó.
    assert tabla.tiempo_scipy_ms.isna().all() and tabla.cociente.isna().all()
    assert tabla.estado_scipy.eq("no completada").all()
