"""``python -m preparacion <comando>`` (ejecutar desde la carpeta python/)."""

from __future__ import annotations

import argparse
import sys
from collections.abc import Callable


def _maquina() -> None:
    from preparacion.maquina import guardar_info_maquina

    for clave, valor in guardar_info_maquina().items():
        print(f"{clave}: {valor}")


def _descargar() -> None:
    from preparacion import red

    print("Preparando la red vial: se reutilizará una copia válida si ya existe...", flush=True)
    red.descargar()
    print(f"Red disponible: {red.DATA_RAW / 'quindio.graphml'}", flush=True)


def _procesar() -> None:
    from preparacion import red
    import pandas as pd

    red.procesar()
    print(f"Red procesada disponible: {red.DATA_PROCESSED}")
    lugares = pd.read_csv(red.DATA_PROCESSED / "lugares_utm.csv")
    catalogo = pd.read_csv(red.CATALOGO)
    if len(lugares) < len(catalogo):
        print(f"Aviso: {len(catalogo) - len(lugares)} lugares sin coordenadas completas; "
              "su asociación a la red sigue pendiente.")


def _perezoso(modulo: str, funcion: str) -> Callable[[], None]:
    def ejecutar() -> None:
        import importlib

        getattr(importlib.import_module(modulo), funcion)()

    return ejecutar


COMANDOS: dict[str, tuple[Callable[[], None], str]] = {
    "maquina": (_maquina, "results/maquina_python.csv (protocolo 6.1)"),
    "descargar": (_descargar, "Red del Quindío (internet solo si no hay copia válida)"),
    "procesar": (_procesar, "Pesos, componente y exportación"),
    "r1": (_perezoso("preparacion.r1_componente", "ejecutar"), "R1.3: tiempo de la componente"),
    "verificar-lugares": (_perezoso("preparacion.verificaciones", "verificar_lugares"),
                          "R1.4 contra ox.distance.nearest_nodes"),
    "verificar-dijkstra": (_perezoso("preparacion.verificaciones", "verificar_dijkstra"),
                           "R2 contra NetworkX"),
    "envolvente-scipy": (_perezoso("preparacion.envolvente_scipy", "ejecutar"),
                         "R3: verificación y tiempos de SciPy"),
    "graficas": (_perezoso("preparacion.graficas", "ejecutar"), "Figuras desde results/*.csv"),
}


def main() -> None:
    parser = argparse.ArgumentParser(prog="python -m preparacion")
    sub = parser.add_subparsers(dest="comando", required=True)
    for nombre, (_, ayuda) in COMANDOS.items():
        sub.add_parser(nombre, help=ayuda)
    args = parser.parse_args()
    try:
        COMANDOS[args.comando][0]()
    except NotImplementedError as pendiente:
        print(f"Pendiente de implementar: {pendiente}")
        raise SystemExit(2) from None
    except (OSError, ValueError, RuntimeError) as error:
        print(f"No se pudo ejecutar '{args.comando}': {error}", file=sys.stderr)
        raise SystemExit(1) from None


if __name__ == "__main__":
    main()
