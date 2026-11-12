"""R1.3 — Tiempo de ``ox.truncate.largest_component`` en redes de tamaño creciente.

Spec: specs/R1-modelo-red.md. Redes: ``ox.graph_from_point`` desde la plaza de Bolívar de
Armenia (coordenadas de ``cab_armenia`` en el catálogo) con radios RADIOS_KM, más la red del
departamento. Se medirá con ``preparacion.medicion.medir_con_limite`` (5 repeticiones,
mediana y corte por ejecución). Integración aún pendiente.

Salidas:
- results/r1/tamano_red.csv          red, nodos, aristas  (descargada y componente)
- results/r1/tiempo_componente.csv   red, V, E, V_mas_E, tiempo_ms, repeticiones, estado
"""

from __future__ import annotations


def ejecutar() -> None:
    raise NotImplementedError("R1.3 — ver specs/R1-modelo-red.md")
