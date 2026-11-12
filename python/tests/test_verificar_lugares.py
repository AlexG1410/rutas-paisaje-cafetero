"""R1.4: oráculo OSMnx con CSV temporales, sin peticiones HTTP."""

import pandas as pd
import pytest

from preparacion import verificaciones as ver
from preparacion.proyeccion import a_utm


@pytest.fixture
def entorno(tmp_path, monkeypatch):
    procesados = tmp_path / "processed"
    procesados.mkdir()
    resultados = tmp_path / "results"
    catalogo = tmp_path / "catalogo.csv"
    monkeypatch.setattr(ver, "DATA_PROCESSED", procesados, raising=False)
    monkeypatch.setattr(ver, "RESULTS", resultados, raising=False)
    monkeypatch.setattr(ver, "CATALOGO", catalogo, raising=False)
    x, y = a_utm(4.5, -75.7)
    pd.DataFrame({"idx": [0, 1], "x_utm": [x, x + 1000], "y_utm": [y, y]}).to_csv(
        procesados / "nodos.csv.gz", index=False)
    pd.DataFrame({"id": ["uno"], "lat": [4.5], "lon": [-75.7]}).to_csv(catalogo, index=False)
    pd.DataFrame({"id_lugar": ["uno"], "x_utm": [x], "y_utm": [y]}).to_csv(
        procesados / "lugares_utm.csv", index=False)
    pd.DataFrame({"id_lugar": ["uno"], "idx_nodo": [0], "distancia_m": [0.0],
                  "mas_de_500m": [False], "tratamiento": [""]}).to_csv(
        procesados / "lugares_nodos.csv", index=False)
    return procesados, resultados / "r1/verificacion_lugar_nodo.csv"


def test_t1va_oraculo_real(entorno):
    _, salida = entorno
    ver.verificar_lugares()
    filas = pd.read_csv(salida)
    assert filas.columns.tolist() == ["id_lugar", "idx_java", "idx_osmnx", "coinciden"]
    assert filas.idx_osmnx.tolist() == [0]
    assert filas.coinciden.tolist() == [True]


@pytest.mark.parametrize(("campo", "valor"), [
    ("idx_nodo", 1), ("distancia_m", 20), ("mas_de_500m", True),
])
def test_t1vb_d_discrepancia_genera_reporte_y_error(entorno, campo, valor):
    procesados, salida = entorno
    archivo = procesados / "lugares_nodos.csv"
    filas = pd.read_csv(archivo)
    filas[campo] = valor
    filas.to_csv(archivo, index=False)
    with pytest.raises(ValueError, match="Discrepancias"):
        ver.verificar_lugares()
    assert pd.read_csv(salida).coinciden.tolist() == [False]


@pytest.mark.parametrize("caso", ["duplicado", "id_ajeno", "indice_fuera", "utm_viejo", "nan"])
def test_t1vc_datos_invalidos_no_generan_reporte(entorno, caso):
    procesados, salida = entorno
    archivo = procesados / ("lugares_utm.csv" if caso == "utm_viejo" else "lugares_nodos.csv")
    filas = pd.read_csv(archivo)
    if caso == "duplicado":
        filas = pd.concat([filas, filas])
    elif caso == "id_ajeno":
        filas["id_lugar"] = "ajeno"
    elif caso == "indice_fuera":
        filas["idx_nodo"] = 99
    elif caso == "utm_viejo":
        filas["x_utm"] += 100
    else:
        filas["distancia_m"] = float("nan")
    filas.to_csv(archivo, index=False)
    with pytest.raises(ValueError):
        ver.verificar_lugares()
    assert not salida.exists()
