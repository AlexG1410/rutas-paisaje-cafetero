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

import math

import matplotlib.pyplot as plt
import pandas as pd

from preparacion.config import FIGURAS, RESULTS


def ejecutar() -> None:
    """Genera la figura R1 disponible; R2–R4 se integrarán con sus experimentos."""
    archivo = RESULTS / "r1" / "tiempo_componente.csv"
    if not archivo.exists():
        raise FileNotFoundError(f"No existe el resultado experimental: {archivo}")
    tabla = pd.read_csv(archivo)
    requeridas = {"red", "V_mas_E", "tiempo_ms", "estado"}
    if not requeridas.issubset(tabla.columns):
        faltantes = ", ".join(sorted(requeridas - set(tabla.columns)))
        raise ValueError(f"Faltan columnas en {archivo}: {faltantes}")
    if tabla.empty or not tabla["estado"].isin(["completada", "no completada"]).all():
        raise ValueError(f"Datos vacíos o estado inválido en {archivo}")
    completadas = tabla.loc[tabla["estado"] == "completada"].sort_values("V_mas_E")
    no_completadas = tabla.loc[tabla["estado"] == "no completada"]
    if not all(math.isfinite(float(valor)) and float(valor) > 0
               for valor in tabla["V_mas_E"]):
        raise ValueError(f"Tamaño no finito o no positivo en {archivo}")
    if not all(math.isfinite(float(valor)) and float(valor) >= 0
               for valor in completadas["tiempo_ms"]):
        raise ValueError(f"Datos no finitos o vacíos en {archivo}")
    if no_completadas["tiempo_ms"].notna().any():
        raise ValueError(f"Una ejecución no completada no debe tener tiempo en {archivo}")

    FIGURAS.mkdir(parents=True, exist_ok=True)
    figura, eje = plt.subplots(figsize=(8, 5))
    try:
        eje.plot(completadas["V_mas_E"], completadas["tiempo_ms"], marker="o",
                 label="Mediana de ejecuciones completadas")
        for posicion, (_, fila) in enumerate(completadas.iterrows()):
            # Separar etiquetas de puntos próximos, como 20 km y departamento.
            eje.annotate(fila["red"], (fila["V_mas_E"], fila["tiempo_ms"]),
                         xytext=(5, 10 if posicion % 2 == 0 else -16),
                         textcoords="offset points")
        for posicion, (_, fila) in enumerate(no_completadas.iterrows()):
            # Y es una fracción de la altura del eje, NO un tiempo experimental cero.
            eje.plot(fila["V_mas_E"], 0.05, marker="x", color="red",
                     transform=eje.get_xaxis_transform(),
                     label="No completada (sin tiempo)" if posicion == 0 else None)
            eje.annotate(f"{fila['red']}: no completada", (fila["V_mas_E"], 0.05),
                         xycoords=("data", "axes fraction"), xytext=(5, 5),
                         textcoords="offset points")
        eje.set_xlim(0, float(tabla["V_mas_E"].max()) * 1.3)
        eje.margins(y=0.15)
        if completadas.empty:
            eje.set_yticks([])
        eje.set_title("Tiempo de selección de la componente fuerte")
        eje.set_xlabel("V + E (número de elementos)")
        eje.set_ylabel("Tiempo (ms)")
        eje.grid(True, alpha=0.3)
        eje.legend()
        figura.tight_layout()
        figura.savefig(FIGURAS / "r1_tiempo_componente.png", dpi=150)
    finally:
        plt.close(figura)
