"""R2: NetworkX real sobre CSV temporales; sin red ni resultados inventados del proyecto."""

import pandas as pd
import pytest

from preparacion import verificaciones as v


@pytest.fixture
def entorno(tmp_path, monkeypatch):
    datos = tmp_path / "processed"
    resultados = tmp_path / "results"
    datos.mkdir()
    (resultados / "r2").mkdir(parents=True)
    catalogo = tmp_path / "catalogo.csv"
    pd.DataFrame({"id": ["a", "b", "c"]}).to_csv(catalogo, index=False)
    pd.DataFrame({"idx": [0, 1, 2]}).to_csv(datos / "nodos.csv.gz", index=False)
    pd.DataFrame({"u": [0, 0, 1, 1, 2], "v": [1, 1, 0, 2, 0],
                  "tiempo_s": [10, 1.23456, 5, 2, 3]}).to_csv(datos / "aristas.csv.gz", index=False)
    pd.DataFrame({"id_lugar": ["a", "b", "c"], "idx_nodo": [0, 1, 2]}).to_csv(
        datos / "lugares_nodos.csv", index=False)
    pd.DataFrame({"origen": ["a"], "destino": ["c"], "nodo_origen": [0],
                  "nodo_destino": [2], "medir_lineal": [True]}).to_csv(
        resultados / "r2/pares.csv", index=False)
    pd.DataFrame({"origen": ["a", "a"], "destino": ["c", "c"],
                  "version": ["lineal", "cola_prioridad"], "tiempo_viaje_s": [3.23456] * 2,
                  "nodos_explorados": [3] * 2, "tiempo_ms": [0.5, 0.1],
                  "repeticiones": [5] * 2, "estado": ["completada"] * 2,
                  "ruta_nodos": ["0|1|2"] * 2}).to_csv(
        resultados / "r2/mediciones_dijkstra.csv", index=False)
    pd.DataFrame({"id": ["a", "b", "c"], "a": [0, 5, 3],
                  "b": [1.235, 0, 4.235], "c": [3.235, 2, 0]}).to_csv(
        resultados / "r2/matriz_tiempos.csv", index=False)
    monkeypatch.setattr(v, "DATA_PROCESSED", datos)
    monkeypatch.setattr(v, "RESULTS", resultados)
    monkeypatch.setattr(v, "CATALOGO", catalogo)
    return datos, resultados / "r2"


def test_t2va_pares_rutas_y_matriz_con_paralelas(entorno):
    _, salida = entorno
    v.verificar_dijkstra()
    pares = pd.read_csv(salida / "verificacion_networkx.csv")
    matriz = pd.read_csv(salida / "verificacion_matriz_networkx.csv")
    assert len(pares) == 2 and pares.coinciden.all()
    assert len(matriz) == 9 and matriz.coinciden.all()


@pytest.mark.parametrize("campo,valor", [("tiempo_viaje_s", 99), ("ruta_nodos", "0|2"),
                                         ("ruta_nodos", "0|1|0|1|2")])
def test_t2vb_discrepancia_de_costo_o_ruta(entorno, campo, valor):
    _, salida = entorno
    ruta = salida / "mediciones_dijkstra.csv"
    tabla = pd.read_csv(ruta)
    tabla.loc[0, campo] = valor
    tabla.to_csv(ruta, index=False)
    with pytest.raises(ValueError):
        v.verificar_dijkstra()


def test_t2vb_matriz_incorrecta_no_se_corrige(entorno):
    _, salida = entorno
    ruta = salida / "matriz_tiempos.csv"
    tabla = pd.read_csv(ruta)
    tabla.loc[0, "b"] = 1.236
    tabla.to_csv(ruta, index=False)
    contenido = ruta.read_bytes()
    with pytest.raises(ValueError, match="Discrepancias"):
        v.verificar_dijkstra()
    assert ruta.read_bytes() == contenido


@pytest.mark.parametrize("problema", ["id", "nodo", "duplicada", "falta_medicion"])
def test_t2vb_archivos_inconsistentes(entorno, problema):
    datos, salida = entorno
    if problema in ("id", "nodo"):
        ruta = datos / "lugares_nodos.csv"
        tabla = pd.read_csv(ruta)
        tabla["idx_nodo"] = tabla["idx_nodo"].astype(float)
        tabla.loc[0, "id_lugar" if problema == "id" else "idx_nodo"] = "otro" if problema == "id" else 0.5
    else:
        ruta = salida / "mediciones_dijkstra.csv"
        tabla = pd.read_csv(ruta)
        tabla = pd.concat([tabla, tabla.iloc[[0]]]) if problema == "duplicada" else tabla.iloc[[0]]
    tabla.to_csv(ruta, index=False)
    with pytest.raises(ValueError):
        v.verificar_dijkstra()


def test_t2va_timeout_no_tiene_resultado_que_verificar(entorno):
    _, salida = entorno
    ruta = salida / "mediciones_dijkstra.csv"
    tabla = pd.read_csv(ruta)
    tabla.loc[0, "estado"] = "no completada"
    tabla.loc[0, "repeticiones"] = 0
    for columna in ("tiempo_viaje_s", "nodos_explorados", "tiempo_ms", "ruta_nodos"):
        tabla[columna] = tabla[columna].astype(object)
        tabla.loc[0, columna] = None
    tabla.to_csv(ruta, index=False)
    v.verificar_dijkstra()
    assert len(pd.read_csv(salida / "verificacion_networkx.csv")) == 1


def test_t2vb_aristas_sin_columnas_dan_error_claro(entorno):
    datos, _ = entorno
    pd.DataFrame({"u": [0], "v": [1]}).to_csv(datos / "aristas.csv.gz", index=False)
    with pytest.raises(ValueError, match="columnas"):
        v.verificar_dijkstra()
