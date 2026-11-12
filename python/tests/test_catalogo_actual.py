"""T1.C-a: regresión de los puntos y del formato del catálogo acordado."""

import csv
import re

from preparacion.config import CATALOGO


def test_t1ca_catalogo_conserva_puntos_y_notas_de_entrega():
    with CATALOGO.open(encoding="utf-8", newline="") as archivo:
        filas = list(csv.DictReader(archivo))
    assert len(filas) == 40
    por_id = {fila["id"]: fila for fila in filas}
    puntos = {
        "atr_valle_cocora": (4.637694, -75.487470),
        "atr_recuca": (4.4326046, -75.7332347),
        "atr_penas_blancas": (4.481609, -75.634164),
        "fh_combia": (4.491763, -75.686133),
        "ac_central_el_agrado": (4.5180079, -75.7926973),
    }
    for identificador, coordenadas in puntos.items():
        fila = por_id[identificador]
        assert (float(fila["lat"]), float(fila["lon"])) == coordenadas
    for fila in filas:
        assert re.fullmatch(
            r"Google Maps; revisado en mapa por el grupo \(\d{4}-\d{2}-\d{2}\)",
            fila["fuente_coordenadas"],
        )
        for expresion in ("integrante", "por el agente", "sin verificación independiente",
                          "último acceso no modelado", "pendiente:"):
            assert expresion not in fila["notas"].lower()
