package co.edu.uniquindio.rutas.service;

import static org.junit.jupiter.api.Assertions.*;

import co.edu.uniquindio.rutas.model.Lugar;
import co.edu.uniquindio.rutas.repository.MatrizRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItinerarioServiceTest {
    private ItinerarioService servicio(int cantidad, int visitaMin, double viajeS) {
        List<String> ids = new ArrayList<>(List.of("p"));
        List<Lugar> catalogo = new ArrayList<>(List.of(new Lugar("p", "Partida", "m", "cabecera", 4.0, -75.0, null)));
        for (int i = 0; i < cantidad; i++) {
            ids.add("a" + i);
            catalogo.add(new Lugar("a" + i, "Atractivo " + i, "m", "atractivo", 4.0, -75.0, visitaMin));
        }
        double[][] matriz = new double[ids.size()][ids.size()];
        for (int i = 0; i < ids.size(); i++) for (int j = 0; j < ids.size(); j++) {
            matriz[i][j] = i == j ? 0 : viajeS;
        }
        return new ItinerarioService(new MatrizRepository() {
            @Override public MatrizLeida leer() { return new MatrizLeida(ids, matriz); }
        }, new CatalogoService(null) {
            @Override public List<Lugar> lugares() { return catalogo; }
        });
    }

    @Test void t4ia_totalesRegresoYBordes() {
        var servicio = servicio(2, 30, 600);
        var cero = servicio.planear("p", List.of());
        assertEquals(0, cero.duracionJornadaMin());
        assertEquals(1, cero.paradas().size());
        assertEquals("p", cero.paradas().getLast().idLugar());
        var uno = servicio.planear("p", List.of("a1"));
        assertEquals(30, uno.totalVisitasMin());
        assertEquals(20, uno.totalDesplazamientosMin());
        assertEquals(50, uno.duracionJornadaMin());
        assertEquals(50.0 / 60, uno.paradas().getLast().tiempoAcumuladoH(), 1e-10);
        assertEquals(0, uno.paradas().getLast().visitaMin());
        assertEquals(1, uno.paradas().getFirst().orden());
    }

    @Test void t4ia_limiteInclusivoYRetirosConEmpate() {
        assertTrue(servicio(2, 315, 600).planear("p", List.of("a0", "a1")).retirados().isEmpty());
        var finalizado = servicio(8, 300, 600).planear("p",
                List.of("a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"));
        assertEquals(List.of("a0", "a1", "a2", "a3", "a4", "a5"), finalizado.retirados());
        assertEquals(630, finalizado.duracionJornadaMin());
        assertEquals(3, finalizado.paradas().size());
        var vacio = servicio(1, 700, 600).planear("p", List.of("a0"));
        assertEquals(List.of("a0"), vacio.retirados());
        assertEquals(0, vacio.duracionJornadaMin());
    }

    @Test void t4ia_comparacionNoRetiraYNulosDePorcentaje() {
        var comparacion = servicio(2, 400, 600).comparar("p", List.of("a1", "a0"));
        assertEquals(830, comparacion.optimo().duracionJornadaMin());
        assertTrue(comparacion.optimo().retirados().isEmpty());
        assertEquals(0, comparacion.diferenciaMin());
        assertEquals(0, comparacion.diferenciaPct());
        assertEquals(0, servicio(0, 1, 0).comparar("p", List.of()).diferenciaPct());
    }

    @Test void t4ib_solicitudesInvalidas() {
        var servicio = servicio(2, 30, 60);
        assertThrows(IllegalArgumentException.class, () -> servicio.planear("p", null));
        for (var ids : List.of(List.of("a0", "a0"), List.of("p"), List.of("desconocido"))) {
            assertThrows(IllegalArgumentException.class, () -> servicio.planear("p", ids));
        }
        assertThrows(IllegalArgumentException.class, () -> servicio.planear("otra", List.of()));
        assertThrows(IllegalArgumentException.class, () -> servicio(1, 0, 0).planear("p", List.of("a0")));
    }

    @Test void t4ib_datosIncoherentes() {
        var servicio = new ItinerarioService(new MatrizRepository() {
            @Override public MatrizLeida leer() { return new MatrizLeida(List.of("p", "x"), new double[2][2]); }
        }, new CatalogoService(null) {
            @Override public List<Lugar> lugares() { return List.of(new Lugar("p", "p", "m", "cabecera", null, null, null)); }
        });
        assertThrows(IllegalStateException.class, () -> servicio.planear("p", List.of()));
    }

    @Test void t4ib_nulosYValoresNoFinitos() {
        var lugares = List.of(new Lugar("p", "p", "m", "cabecera", null, null, null));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.validarDatos(null, lugares));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.validarDatos(
                new MatrizRepository.MatrizLeida(null, new double[1][1]), lugares));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.validarDatos(
                new MatrizRepository.MatrizLeida(List.of("p"), null), lugares));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.validarDatos(
                new MatrizRepository.MatrizLeida(java.util.Arrays.asList((String) null), new double[1][1]), lugares));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.validarDatos(
                new MatrizRepository.MatrizLeida(List.of("p"), new double[][] {null}), lugares));
        for (double malo : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1, 1}) {
            assertThrows(IllegalStateException.class, () -> ItinerarioService.validarDatos(
                    new MatrizRepository.MatrizLeida(List.of("p"), new double[][] {{malo}}), lugares));
        }
        assertThrows(IllegalArgumentException.class,
                () -> servicio(1, 30, 60).planear("p", java.util.Arrays.asList((String) null)));
    }

    @Test void t4ib_brechaSinDesbordesNiPorcentajesFicticios() {
        assertEquals(100, ItinerarioService.brecha(Double.MAX_VALUE / 2, Double.MAX_VALUE));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.brecha(Double.MIN_VALUE, 1));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.brecha(0, 1));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.brecha(1, -1));
        assertThrows(IllegalStateException.class, () -> ItinerarioService.brecha(1, Double.NaN));
    }
}
