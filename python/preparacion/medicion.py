"""Arnés de medición en Python (protocolo sección 6), con la MISMA política que el Medidor de
Java para que las comparaciones sean justas:

- solo se mide la llamada ``fn(*args)``; preparar datos queda fuera;
- ``calentamientos`` ejecuciones previas que no se cuentan; si una pasa de 1 min, se toma
  como la única medición;
- luego ``repeticiones`` ejecuciones medidas (mediana), en ms con ``perf_counter_ns``;
- cualquier repetición que supere 1 min detiene la serie y queda como única muestra;
- ``gc.collect()`` antes de cada ejecución.

Preparado para R1.3 (``ox.truncate.largest_component``) y SciPy en R3; integrar cada
experimento con ``medir_con_limite`` antes de ejecutarlo.
"""

from __future__ import annotations

import gc
import math
import multiprocessing as mp
import pickle
import statistics
import time
from collections.abc import Callable
from dataclasses import dataclass, field
from multiprocessing.connection import Connection
from typing import Any

from preparacion.config import (
    CALENTAMIENTOS, LIMITE_TIEMPO_S, REPETICIONES, UMBRAL_UNA_SOLA_MEDICION_S,
)

# Espera de infraestructura, distinta del límite de una ejecución del algoritmo.
_ESPERA_INFRAESTRUCTURA_S = 60.0


@dataclass
class Medicion:
    tiempos_ms: list[float] = field(default_factory=list)
    completada: bool = True
    resultado: Any = None

    @property
    def mediana_ms(self) -> float | None:
        if not self.completada or not self.tiempos_ms:
            return None
        return statistics.median(self.tiempos_ms)

    @property
    def repeticiones(self) -> int:
        return len(self.tiempos_ms)

    def como_fila(self) -> dict[str, Any]:
        return {
            "tiempo_ms": self.mediana_ms,
            "repeticiones": self.repeticiones,
            "estado": "completada" if self.completada else "no completada",
        }


def _una_ejecucion(fn: Callable[..., Any], args: tuple, kwargs: dict) -> tuple[float, Any]:
    gc.collect()
    return _cronometrar(fn, args, kwargs)


def _cronometrar(fn: Callable[..., Any], args: tuple, kwargs: dict) -> tuple[float, Any]:
    """Único tramo cronometrado; GC, mensajes y serialización no entran aquí."""
    inicio = time.perf_counter_ns()
    resultado = fn(*args, **kwargs)
    fin = time.perf_counter_ns()
    return (fin - inicio) / 1e6, resultado


def medir(
    fn: Callable[..., Any],
    *args: Any,
    repeticiones: int = REPETICIONES,
    calentamientos: int = CALENTAMIENTOS,
    umbral_una_s: float = UMBRAL_UNA_SOLA_MEDICION_S,
    **kwargs: Any,
) -> Medicion:
    """Mide directamente, sin corte; los experimentos deben usar medir_con_limite."""
    return _medir_serie(fn, args, kwargs, repeticiones, calentamientos, umbral_una_s)


def _medir_serie(
    fn: Callable[..., Any], args: tuple, kwargs: dict,
    repeticiones: int, calentamientos: int, umbral_una_s: float,
    canal: Connection | None = None,
) -> Medicion:
    """Política común para ejecución directa y aislada, sin duplicar las reglas 6.3."""
    umbral_ms = umbral_una_s * 1000
    medicion = Medicion()
    for numero in range(calentamientos + repeticiones):
        es_calentamiento = numero < calentamientos
        if canal is None:
            ms, medicion.resultado = _una_ejecucion(fn, args, kwargs)
        else:
            gc.collect()
            canal.send(("INICIO", None))
            ms, medicion.resultado = _cronometrar(fn, args, kwargs)
            canal.send(("CALENTAMIENTO" if es_calentamiento else "REPETICION", ms))
        if ms > umbral_ms:
            # La ejecución larga sustituye las muestras breves previas (protocolo 6.3).
            medicion.tiempos_ms = [ms]
            return medicion
        if not es_calentamiento:
            medicion.tiempos_ms.append(ms)
    return medicion


def _trabajo_aislado(
    canal: Connection, entrada: bytes,
    repeticiones: int, calentamientos: int, umbral_una_s: float,
) -> None:
    """Punto de entrada importable por spawn; no se llama desde la CLI directamente."""
    try:
        fn, args, kwargs = pickle.loads(entrada)  # Preparación antes de INICIO y del reloj.
        medicion = _medir_serie(fn, args, kwargs, repeticiones, calentamientos,
                               umbral_una_s, canal)
        canal.send(("RESULTADO", medicion))
    except BaseException as error:
        # Solo enviar texto: incluso la excepción de la tarea puede no ser serializable.
        try:
            canal.send(("ERROR", f"{type(error).__name__}: {error}"))
        except (BrokenPipeError, EOFError, OSError):
            pass  # El padre ya cerró el canal al cancelar o terminar la tarea.
    finally:
        canal.close()


def _validar_parametros(
    fn: Callable[..., Any], repeticiones: int, calentamientos: int,
    umbral_una_s: float, limite_s: float,
) -> None:
    if not callable(fn):
        raise ValueError("La tarea debe ser invocable")
    if type(repeticiones) is not int or repeticiones <= 0:
        raise ValueError("repeticiones debe ser entero positivo")
    if type(calentamientos) is not int or calentamientos < 0:
        raise ValueError("calentamientos debe ser entero no negativo")
    for nombre, valor in (("umbral_una_s", umbral_una_s), ("limite_s", limite_s)):
        if type(valor) not in (int, float) or not math.isfinite(valor) or valor <= 0:
            raise ValueError(f"{nombre} debe ser finito y positivo")


def medir_con_limite(
    fn: Callable[..., Any],
    *args: Any,
    repeticiones: int = REPETICIONES,
    calentamientos: int = CALENTAMIENTOS,
    umbral_una_s: float = UMBRAL_UNA_SOLA_MEDICION_S,
    limite_s: float = LIMITE_TIEMPO_S,
    **kwargs: Any,
) -> Medicion:
    """Protocolo 6.7: serie en un hijo spawn, límite por ejecución y limpieza garantizada.

    La tarea y sus datos deben ser serializables. Preparación/arranque/transferencias
    no se cronometran; el hijo solo mide fn(*args, **kwargs). El padre puede terminarlo
    aunque la tarea no coopere. Los errores de tarea o infraestructura se propagan como
    RuntimeError, no como resultados experimentales «no completada».
    """
    _validar_parametros(fn, repeticiones, calentamientos, umbral_una_s, limite_s)
    try:
        # Windows crea el hijo antes de terminar de serializar Process. No enviar una
        # tarea inválida por ese canal: podría dejar un arranque parcial sin PID recogible.
        entrada = bytes(mp.reduction.ForkingPickler.dumps((fn, args, kwargs)))
    except Exception as error:
        raise RuntimeError("No se pudo iniciar el proceso; use tarea y datos serializables") from error
    contexto = mp.get_context("spawn")
    receptor, emisor = contexto.Pipe(duplex=False)
    proceso = contexto.Process(target=_trabajo_aislado, args=(
        emisor, entrada, repeticiones, calentamientos, umbral_una_s,
    ), daemon=True)
    tiempos: list[float] = []
    ejecutando = False
    try:
        try:
            proceso.start()
        except Exception as error:
            raise RuntimeError(
                "No se pudo iniciar el proceso; use tarea y datos serializables"
            ) from error
        # Cerrar esta copia permite detectar EOF si el hijo muere sin enviar resultado.
        emisor.close()
        while True:
            espera_s = limite_s if ejecutando else _ESPERA_INFRAESTRUCTURA_S
            if not receptor.poll(espera_s):
                if ejecutando:
                    return Medicion(tiempos, False, None)
                raise RuntimeError("El proceso no respondió durante preparación o transferencia")
            try:
                tipo, valor = receptor.recv()
            except (EOFError, OSError) as error:
                raise RuntimeError("El proceso terminó sin devolver un resultado") from error
            except Exception as error:
                raise RuntimeError("No se pudo deserializar el resultado del proceso") from error
            if tipo == "INICIO":
                ejecutando = True
            elif tipo in ("CALENTAMIENTO", "REPETICION"):
                ejecutando = False
                # El padre puede recibir tarde un mensaje; manda también la duración real.
                if valor > limite_s * 1000:
                    return Medicion(tiempos, False, None)
                if valor > umbral_una_s * 1000:
                    tiempos = [valor]
                elif tipo == "REPETICION":
                    tiempos.append(valor)
            elif tipo == "RESULTADO":
                return valor
            elif tipo == "ERROR":
                raise RuntimeError(f"La tarea del proceso falló: {valor}")
            else:
                raise RuntimeError(f"Mensaje desconocido del proceso: {tipo}")
    finally:
        receptor.close()
        emisor.close()
        if proceso.pid is not None:
            # Después de recibir el resultado ya no queda trabajo útil en el hijo.
            if proceso.is_alive():
                proceso.terminate()
            proceso.join(timeout=5)
            if proceso.is_alive():
                proceso.kill()
                proceso.join(timeout=5)
            if proceso.is_alive():
                raise RuntimeError("No se pudo terminar el proceso de medición")
        proceso.close()
