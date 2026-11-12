"""Figuras del documento técnico y de la app, SOLO a partir de results/*.csv (protocolo 6.8).

- results/figuras/r1_tiempo_componente.png     V + E vs tiempo (ms)
- results/figuras/r2_tiempo_vs_explorados.png  nodos explorados vs tiempo (ms), una serie
                                               por versión (lineal / cola_prioridad)
- results/figuras/r3_tiempos.png               (opcional) propio vs SciPy por conjunto
- results/figuras/r4_tiempo_vs_k.png           k vs tiempo (ms), una curva por método, eje y
                                               logarítmico, "no completadas" marcadas
Cada figura: título, ejes con unidad, leyenda; PNG a 150 dpi.

Contrato de R1.1 para futuros consumidores: results/r1/memoria_representacion.csv
tiene red,representacion,supuesto_bytes,V,E,megabytes (ampliado con red, 2026-11-11).
Seis filas: matriz/listas por descargada, componente_exportada y grafo_java.
MB decimales estimados con modelo compacto; no son mediciones de memoria de la JVM.
Este módulo todavía no lee ni grafica ese archivo.
"""

from __future__ import annotations


def ejecutar() -> None:
    raise NotImplementedError("Figuras — ver specs/protocolo-experimental.md")
