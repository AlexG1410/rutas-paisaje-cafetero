"""Protocolo 6.7: procesos spawn reales y tareas sintéticas, no experimentos."""

import multiprocessing as mp
import os
import time
from collections.abc import Callable
from pathlib import Path

import pytest

from preparacion import medicion


def _suma_con_pid(n: int, *, incremento: int = 0) -> tuple[int, int]:
    return sum(range(n)) + incremento, os.getpid()


def _fallar() -> None:
    raise ValueError("fallo deliberado de prueba")


def _salir() -> None:
    os._exit(7)


def _resultado_no_serializable() -> Callable:
    return lambda: 1


def _fallar_al_reconstruir() -> None:
    raise ValueError("resultado no reconstruible")


class _ResultadoInvalido:
    def __reduce__(self) -> tuple:
        return _fallar_al_reconstruir, ()


def _resultado_no_reconstruible() -> _ResultadoInvalido:
    return _ResultadoInvalido()


def _dormir_y_escribir(archivo: Path) -> str:
    time.sleep(2)
    archivo.write_text("no debe llegar aquí", encoding="utf-8")
    return "terminó"


class _SerieDemoras:
    """El estado de la tarea persiste entre calentamiento y repeticiones del hijo."""

    def __init__(self, demoras: list[float]) -> None:
        self.demoras = demoras
        self.llamadas = 0

    def __call__(self) -> int:
        demora = self.demoras[self.llamadas]
        self.llamadas += 1
        time.sleep(demora)
        return self.llamadas


def _preparar_lentamente() -> int:
    time.sleep(0.3)
    return 123


class _EntradaLenta:
    def __reduce__(self) -> tuple:
        return _preparar_lentamente, ()


def _identidad(valor: int) -> int:
    return valor


def _hijos() -> set[int]:
    return {hijo.pid for hijo in mp.active_children()}


def test_p67a_cinco_muestras_args_kwargs_pid_y_limpieza() -> None:
    antes = _hijos()
    m = medicion.medir_con_limite(_suma_con_pid, 1000, incremento=5)
    assert m.completada and m.repeticiones == 5
    assert m.resultado[0] == 499_505
    assert m.resultado[1] != os.getpid()
    assert min(m.tiempos_ms) <= m.mediana_ms <= max(m.tiempos_ms)
    assert _hijos() == antes


def test_p67a_limite_por_ejecucion_no_por_suma_de_la_serie() -> None:
    m = medicion.medir_con_limite(_SerieDemoras([0.05] * 6), limite_s=0.2)
    assert m.completada and m.repeticiones == 5 and m.resultado == 6


@pytest.mark.parametrize("calentamientos", [0, 1])
def test_p67b_corta_tarea_y_no_deja_proceso_activo(tmp_path: Path, calentamientos: int) -> None:
    antes = _hijos()
    archivo = tmp_path / "no_debe_existir.txt"
    m = medicion.medir_con_limite(_dormir_y_escribir, archivo,
                                calentamientos=calentamientos, limite_s=0.1)
    assert not m.completada and m.repeticiones == 0
    assert m.resultado is None and m.mediana_ms is None
    assert m.como_fila() == {"tiempo_ms": None, "repeticiones": 0, "estado": "no completada"}
    assert _hijos() == antes
    assert not archivo.exists()


def test_p67c_timeout_posterior_no_publica_mediana_parcial() -> None:
    antes = _hijos()
    m = medicion.medir_con_limite(_SerieDemoras([0, 0, 2]), limite_s=0.2)
    assert not m.completada and m.repeticiones == 1
    assert m.resultado is None and m.mediana_ms is None
    assert m.como_fila()["tiempo_ms"] is None
    assert _hijos() == antes


@pytest.mark.parametrize("tarea, mensaje", [
    (_fallar, "fallo deliberado"), (_salir, "proceso"),
    (_resultado_no_serializable, "serializ|pickle"),
    (_resultado_no_reconstruible, "deserializar"),
])
def test_p67d_errores_no_se_confunden_con_timeout(tarea: Callable, mensaje: str) -> None:
    antes = _hijos()
    with pytest.raises(RuntimeError, match=mensaje):
        medicion.medir_con_limite(tarea)
    assert _hijos() == antes


def test_p67e_preparacion_fuera_del_limite_del_algoritmo() -> None:
    m = medicion.medir_con_limite(_identidad, _EntradaLenta(),
                                repeticiones=1, calentamientos=0, limite_s=0.1)
    assert m.completada and m.resultado == 123
    assert m.repeticiones == 1 and m.mediana_ms < 100


@pytest.mark.parametrize("opciones", [
    {"repeticiones": 0}, {"repeticiones": 1.5}, {"repeticiones": True},
    {"calentamientos": -1}, {"calentamientos": 0.5}, {"calentamientos": True},
    {"umbral_una_s": 0}, {"umbral_una_s": float("nan")},
    {"limite_s": 0}, {"limite_s": -1}, {"limite_s": float("inf")},
])
def test_p67f_parametros_invalidos_no_crean_hijos(opciones: dict) -> None:
    antes = _hijos()
    with pytest.raises(ValueError):
        medicion.medir_con_limite(_identidad, 1, **opciones)
    assert _hijos() == antes


def test_p67f_tarea_no_invocable() -> None:
    with pytest.raises(ValueError):
        medicion.medir_con_limite(123)


def test_p67f_lambda_no_serializable_limpia_recursos() -> None:
    antes = _hijos()
    with pytest.raises(RuntimeError, match="iniciar"):
        medicion.medir_con_limite(lambda: 1)
    assert _hijos() == antes


def test_p67j_inicio_fallido_no_contamina_spawn_siguiente(monkeypatch: pytest.MonkeyPatch) -> None:
    antes = _hijos()
    original = mp.get_context
    contextos = []

    def registrar(metodo):
        contextos.append(metodo)
        return original(metodo)

    monkeypatch.setattr(mp, "get_context", registrar)
    for numero in range(2):
        with pytest.raises(RuntimeError, match="iniciar"):
            medicion.medir_con_limite(lambda: 1)
        assert len(contextos) == numero  # Ni se prepara un proceso para la tarea inválida.
        m = medicion.medir_con_limite(_identidad, 123, repeticiones=1, calentamientos=0)
        assert m.completada and m.resultado == 123
        assert _hijos() == antes


@pytest.mark.parametrize("demoras", [[0.35], [0, 0.35], [0, 0, 0.35]])
def test_p67g_una_muestra_larga_tras_calentamiento(demoras: list[float]) -> None:
    m = medicion.medir_con_limite(_SerieDemoras(demoras), umbral_una_s=0.2, limite_s=10)
    assert m.completada and m.repeticiones == 1
    assert m.resultado == len(demoras)
    assert m.mediana_ms > 200


def test_p67h_osmnx_real_sin_descargar() -> None:
    import networkx as nx
    import osmnx as ox

    grafo = nx.MultiDiGraph()
    grafo.add_edges_from([(1, 2), (2, 1), (2, 3)])
    m = medicion.medir_con_limite(ox.truncate.largest_component, grafo,
                                strongly=True, repeticiones=2)
    assert m.completada and m.repeticiones == 2
    assert set(m.resultado.nodes) == {1, 2}
    assert set(grafo.nodes) == {1, 2, 3}


def test_p67i_preparacion_sin_respuesta_es_error_no_timeout_experimental(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    antes = _hijos()
    monkeypatch.setattr(medicion, "_ESPERA_INFRAESTRUCTURA_S", 0.05)
    with pytest.raises(RuntimeError, match="preparación o transferencia"):
        medicion.medir_con_limite(_identidad, _EntradaLenta(), limite_s=10)
    assert _hijos() == antes
