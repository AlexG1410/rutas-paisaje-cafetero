"""Protocolo 6.1: máquina y versiones de Python donde se mide R1.3 y SciPy (R3).

Se guarda en results/maquina_python.csv (clave, valor). Java guarda maquina_java.csv.
"""

from __future__ import annotations

import csv
import platform
import subprocess
from datetime import datetime
from importlib import metadata
from pathlib import Path

import psutil

from preparacion.config import RESULTS

LIBRERIAS = ("osmnx", "networkx", "scipy", "numpy", "pandas", "pyproj", "geopandas")


def _nombre_procesador() -> str:
    sistema = platform.system()
    try:
        if sistema == "Linux":
            for linea in Path("/proc/cpuinfo").read_text().splitlines():
                if linea.startswith("model name"):
                    return linea.split(":", 1)[1].strip()
        elif sistema == "Darwin":
            return subprocess.check_output(
                ["sysctl", "-n", "machdep.cpu.brand_string"], text=True
            ).strip()
    except Exception:
        pass
    return platform.processor() or "desconocido"


def info_maquina() -> dict[str, str]:
    info = {
        "fecha": datetime.now().isoformat(timespec="seconds"),
        "procesador": _nombre_procesador(),
        "nucleos_logicos": str(psutil.cpu_count(logical=True)),
        "ram_gb": f"{psutil.virtual_memory().total / 1024**3:.2f}",
        "sistema_operativo": platform.platform(),
        "python": platform.python_version(),
    }
    for lib in LIBRERIAS:
        try:
            info[lib] = metadata.version(lib)
        except metadata.PackageNotFoundError:
            info[lib] = "no instalada"
    return info


def guardar_info_maquina(ruta: Path = RESULTS / "maquina_python.csv") -> dict[str, str]:
    info = info_maquina()
    ruta.parent.mkdir(parents=True, exist_ok=True)
    with ruta.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["clave", "valor"])
        w.writerows(info.items())
    return info
