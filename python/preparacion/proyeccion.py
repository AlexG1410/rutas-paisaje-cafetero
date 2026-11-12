"""Conversión WGS84 (lat, lon) -> UTM zona 18N (metros), EPSG:32618."""

from __future__ import annotations

from collections.abc import Sequence
from functools import lru_cache

from pyproj import Transformer

from preparacion.config import EPSG_UTM_18N


@lru_cache(maxsize=1)
def _transformador() -> Transformer:
    # always_xy=True: el orden de entrada es (lon, lat) y el de salida (x, y).
    return Transformer.from_crs("EPSG:4326", f"EPSG:{EPSG_UTM_18N}", always_xy=True)


def a_utm(lat: float, lon: float) -> tuple[float, float]:
    x, y = _transformador().transform(lon, lat)
    return float(x), float(y)


def a_utm_lote(lats: Sequence[float], lons: Sequence[float]) -> tuple[list[float], list[float]]:
    xs, ys = _transformador().transform(list(lons), list(lats))
    return [float(v) for v in xs], [float(v) for v in ys]
