"""Figuras del documento técnico y de la app, SOLO a partir de results/*.csv (protocolo 6.8).

- results/figuras/r1_tiempo_componente.png     V + E vs tiempo (ms)
- results/figuras/r2_tiempo_vs_explorados.png  nodos explorados vs tiempo (ms), una serie
                                                por versión, eje y logarítmico
- results/r2/normalizacion_lineal.csv         T/(k·V) en ns, análisis derivado sin medir
- results/figuras/r3_tiempos.png               (opcional) propio vs SciPy por conjunto
- results/figuras/r4_tiempo_vs_k.png           k vs tiempo (ms), una curva por método, eje y
                                               logarítmico, "no completadas" marcadas
Cada figura: título, ejes con unidad, leyenda; PNG a 150 dpi.

Contrato de R1.1 para futuros consumidores: results/r1/memoria_representacion.csv
tiene red,representacion,supuesto_bytes,V,E,megabytes (ampliado con red, 2026-11-11).
Seis filas: matriz/listas por descargada, componente_exportada y grafo_java.
MB decimales estimados con modelo compacto; no son mediciones de memoria de la JVM.
Para normalizar R2 se lee V de las filas grafo_java de ese archivo.
"""

from __future__ import annotations

import math

import matplotlib.pyplot as plt
import pandas as pd

from preparacion.config import FIGURAS, RESULTS


def ejecutar() -> None:
    """Genera figuras de R1/R2/R3 disponibles; no exige resultados de bloques futuros."""
    disponibles = False
    for archivo, generar in (("r1/tiempo_componente.csv", graficar_componente),
                             ("r2/mediciones_dijkstra.csv", graficar_dijkstra),
                             ("r3/envolvente.csv", graficar_envolvente)):
        if (RESULTS / archivo).exists():
            generar()
            disponibles = True
    if not disponibles:
        raise FileNotFoundError("No hay CSV experimentales de R1/R2/R3 para graficar")


def graficar_componente() -> None:
    """Figura R1 con tiempos completados y marcas separadas para timeout."""
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


def graficar_dijkstra() -> None:
    """R2: explorados frente a tiempo logarítmico y T/(k·V), sin nuevas mediciones."""
    archivo = RESULTS / "r2/mediciones_dijkstra.csv"
    tabla = pd.read_csv(archivo)
    requeridas = {"origen", "destino", "version", "nodos_explorados", "tiempo_ms", "estado"}
    if tabla.empty or not requeridas.issubset(tabla.columns):
        raise ValueError(f"Tabla vacía o columnas faltantes en {archivo}")
    if (not tabla.version.isin(["lineal", "cola_prioridad"]).all()
            or not tabla.estado.isin(["completada", "no completada"]).all()):
        raise ValueError(f"Versión o estado desconocido en {archivo}")
    completadas = tabla.loc[tabla.estado == "completada"]
    no_completadas = tabla.loc[tabla.estado == "no completada"]
    n = _tamano_grafo_java()
    for fila in completadas.itertuples(index=False):
        if (not math.isfinite(float(fila.tiempo_ms)) or fila.tiempo_ms <= 0
                or not math.isfinite(float(fila.nodos_explorados))
                or not 1 <= fila.nodos_explorados <= n
                or fila.nodos_explorados != int(fila.nodos_explorados)):
            raise ValueError(f"Tiempo o nodos explorados inválidos en {archivo}")
    if no_completadas[["tiempo_ms", "nodos_explorados"]].notna().any().any():
        raise ValueError("Las mediciones no completadas no deben contener cifras de rendimiento")
    normalizacion = tabla.loc[tabla.version == "lineal",
                              ["origen", "destino", "nodos_explorados", "tiempo_ms", "estado"]].copy()
    normalizacion.insert(2, "V", n)
    # Es el tiempo TOTAL normalizado: no aísla inicialización, barrido ni relajaciones.
    normalizacion["ns_por_posicion"] = (
        normalizacion.tiempo_ms * 1e6 / (normalizacion.nodos_explorados * n))
    normalizacion = normalizacion[["origen", "destino", "V", "nodos_explorados",
                                   "tiempo_ms", "ns_por_posicion", "estado"]]
    for valor in normalizacion.loc[normalizacion.estado == "completada", "ns_por_posicion"]:
        if not math.isfinite(float(valor)):
            raise ValueError("Normalización no finita")
    FIGURAS.mkdir(parents=True, exist_ok=True)
    figura, eje = plt.subplots(figsize=(8, 5))
    try:
        for version, etiqueta, marca in (("lineal", "Lineal", "o"),
                                         ("cola_prioridad", "Cola de prioridad", "x")):
            filas = completadas.loc[completadas.version == version]
            eje.scatter(filas.nodos_explorados, filas.tiempo_ms, label=etiqueta, marker=marca)
        if not no_completadas.empty:
            eje.text(0.02, 0.97, f"{len(no_completadas)} no completadas (sin tiempo/nodos)",
                     transform=eje.transAxes, va="top", color="red")
        eje.set_title("Dijkstra: nodos explorados y tiempo de ejecución")
        eje.set_xlabel("Nodos explorados (cantidad)")
        eje.set_yscale("log")
        if completadas.empty:
            # Ventana de dibujo sin datos ni marcas de tiempo, no tiempos experimentales.
            eje.set_ylim(1, 10)
            eje.set_yticks([], minor=False)
            eje.set_yticks([], minor=True)
            eje.text(0.5, 0.5, "Sin mediciones completadas", transform=eje.transAxes,
                     ha="center")
        eje.set_ylabel("Tiempo (ms, escala logarítmica)")
        eje.grid(True, which="both", alpha=0.3)
        eje.legend()
        figura.tight_layout()
        figura.savefig(FIGURAS / "r2_tiempo_vs_explorados.png", dpi=150)
        normalizacion.to_csv(RESULTS / "r2/normalizacion_lineal.csv", index=False)
    finally:
        plt.close(figura)


def _tamano_grafo_java() -> int:
    """Lee el tamaño documentado por Java sin reconstruir ni medir el grafo."""
    archivo = RESULTS / "r1/memoria_representacion.csv"
    tabla = pd.read_csv(archivo)
    if not {"red", "V"}.issubset(tabla.columns):
        raise ValueError(f"Faltan columnas red/V en {archivo}")
    valores = pd.to_numeric(tabla.loc[tabla.red == "grafo_java", "V"], errors="raise")
    if valores.empty or not all(math.isfinite(float(v)) and v > 0 and v == int(v) for v in valores):
        raise ValueError(f"V de grafo_java debe ser entero positivo en {archivo}")
    if valores.nunique() != 1:
        raise ValueError(f"V de grafo_java inconsistente en {archivo}")
    return int(valores.iloc[0])


def graficar_envolvente() -> None:
    """Barras por distribución, sin unir tamaños ni inventar tiempo cero en timeout."""
    tabla = pd.read_csv(RESULTS / "r3/envolvente.csv")
    requeridas = {"conjunto", "tiempo_propio_ms", "tiempo_scipy_ms", "estado_propio", "estado_scipy"}
    if tabla.empty or not requeridas.issubset(tabla.columns) or tabla.conjunto.duplicated().any():
        raise ValueError("Tabla R3 vacía, duplicada o con columnas faltantes")
    for metodo in ("propio", "scipy"):
        for fila in tabla.to_dict("records"):
            estado, tiempo = fila[f"estado_{metodo}"], fila[f"tiempo_{metodo}_ms"]
            if estado == "completada":
                if not math.isfinite(float(tiempo)) or tiempo < 0:
                    raise ValueError("Tiempo R3 inválido")
            elif estado != "no completada" or not pd.isna(tiempo):
                raise ValueError("Estado R3 desconocido o timeout con tiempo ficticio")
    FIGURAS.mkdir(parents=True, exist_ok=True)
    figura, eje = plt.subplots(figsize=(9, 5))
    try:
        for metodo, etiqueta, desplazamiento in (("propio", "QuickHull Java", -0.2),
                                                 ("scipy", "SciPy / Qhull", 0.2)):
            posiciones, tiempos = [], []
            for i, fila in enumerate(tabla.to_dict("records")):
                if fila[f"estado_{metodo}"] == "completada":
                    posiciones.append(i + desplazamiento)
                    tiempos.append(fila[f"tiempo_{metodo}_ms"])
                else:
                    eje.text(i + desplazamiento, 0.05, f"{etiqueta}: no completada",
                             transform=eje.get_xaxis_transform(), rotation=90, va="bottom", color="red")
            eje.bar(posiciones, tiempos, width=0.38, label=etiqueta)
        eje.set_xticks(range(len(tabla)), tabla.conjunto)
        eje.set_ylabel("Tiempo (ms)")
        eje.set_title("Envolvente convexa: tiempos por conjunto")
        eje.grid(True, axis="y", alpha=0.3)
        eje.legend()
        figura.tight_layout()
        figura.savefig(FIGURAS / "r3_tiempos.png", dpi=150)
    finally:
        plt.close(figura)
