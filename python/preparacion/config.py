"""Constantes del módulo Python. Mantener sincronizadas con Constantes.java."""

from __future__ import annotations

from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
DATA = RAIZ / "data"
DATA_RAW = DATA / "raw"
DATA_PROCESSED = DATA / "processed"
CATALOGO = DATA / "catalogo" / "lugares.csv"
RESULTS = RAIZ / "results"
FIGURAS = RESULTS / "figuras"

# --- R1 ---
LUGAR_OSM = "Quindío, Colombia"
TIPO_RED = "drive"
RETENER_TODAS_COMPONENTES = True
SIMPLIFICAR_RED = True
EPSG_UTM_18N = 32618  # WGS 84 / UTM zona 18N
RADIOS_KM = (2, 5, 10, 20)  # alrededor de la plaza de Bolívar de Armenia (cab_armenia)
ID_PLAZA_BOLIVAR = "cab_armenia"

# --- Protocolo experimental (sección 6), igual que en Java ---
REPETICIONES = 5
CALENTAMIENTOS = 1
UMBRAL_UNA_SOLA_MEDICION_S = 60.0
LIMITE_TIEMPO_S = 300.0
SEMILLA = 2026
