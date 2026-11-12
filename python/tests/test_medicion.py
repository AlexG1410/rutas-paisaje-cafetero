import time

from preparacion.maquina import info_maquina
from preparacion.medicion import medir
from preparacion.proyeccion import a_utm


def _suma(n):
    return sum(range(n))


def _dormir(segundos):
    time.sleep(segundos)
    return "ok"


def test_cinco_repeticiones_medidas_sin_contar_el_calentamiento():
    m = medir(_suma, 10_000)
    assert m.repeticiones == 5 and m.completada
    assert m.resultado == sum(range(10_000))
    assert min(m.tiempos_ms) <= m.mediana_ms <= max(m.tiempos_ms)


def test_una_sola_medicion_si_el_calentamiento_pasa_el_umbral():
    m = medir(_dormir, 0.05, umbral_una_s=0.01)
    assert m.repeticiones == 1


def test_sin_calentamiento_tambien_respeta_el_umbral():
    m = medir(_dormir, 0.03, calentamientos=0, umbral_una_s=0.01)
    assert m.repeticiones == 1


def test_info_maquina_tiene_campos_del_protocolo():
    info = info_maquina()
    for clave in ("procesador", "ram_gb", "sistema_operativo", "python"):
        assert info[clave]


def test_utm_18n_en_rango_para_armenia():
    x, y = a_utm(4.533, -75.672)  # el meridiano central de la zona 18 es -75°
    assert 400_000 < x < 450_000
    assert 490_000 < y < 515_000
