"""Arnés de medición en Python (protocolo sección 6), con la MISMA política que el Medidor de
Java para que las comparaciones sean justas:

- solo se mide la llamada ``fn(*args)``; preparar datos queda fuera;
- ``calentamientos`` ejecuciones previas que no se cuentan; si una pasa de 1 min, se toma
  como la única medición;
- luego ``repeticiones`` ejecuciones medidas (mediana), en ms con ``perf_counter_ns``;
- ``gc.collect()`` antes de cada ejecución.

Se usa para R1.3 (``ox.truncate.largest_component``) y para SciPy en R3.
"""

from __future__ import annotations

import gc
import statistics
import time
from collections.abc import Callable
from dataclasses import dataclass, field
from typing import Any

from preparacion.config import CALENTAMIENTOS, REPETICIONES, UMBRAL_UNA_SOLA_MEDICION_S


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
    umbral_ms = umbral_una_s * 1000
    medicion = Medicion()
    for _ in range(calentamientos):
        ms, medicion.resultado = _una_ejecucion(fn, args, kwargs)
        if ms > umbral_ms:
            medicion.tiempos_ms.append(ms)
            return medicion
    for i in range(repeticiones):
        ms, medicion.resultado = _una_ejecucion(fn, args, kwargs)
        medicion.tiempos_ms.append(ms)
        if i == 0 and calentamientos == 0 and ms > umbral_ms:
            break
    return medicion
