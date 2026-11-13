package co.edu.uniquindio.rutas.service;

import static org.junit.jupiter.api.Assertions.*;

import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.model.Lugar;
import co.edu.uniquindio.rutas.repository.FilaLugarNodo;
import co.edu.uniquindio.rutas.repository.LugaresNodosRepository;
import co.edu.uniquindio.rutas.repository.RedLeida;
import co.edu.uniquindio.rutas.repository.RedRepository;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RutaServiceTest {
    private Grafo grafo() {
        return Grafo.desdeArreglos(3, new int[] {0, 0, 1, 2}, new int[] {2, 1, 2, 0},
                new double[] {20, 2, 3, 9}, new double[] {20, 2, 3, 9}, null, null, null,
                new double[] {4.5, 4.6, 4.7}, new double[] {-75.7, -75.6, -75.5});
    }

    private List<FilaLugarNodo> asociaciones() {
        return List.of(new FilaLugarNodo("a", 0, 0, false, ""),
                new FilaLugarNodo("b", 2, 800, true, "Alerta conservada"),
                new FilaLugarNodo("c", 0, 0, false, ""));
    }

    private RutaService servicio(Grafo grafo, List<FilaLugarNodo> filas, boolean existe) {
        return new RutaService(new RedService(null) {
            @Override public Grafo grafo() { return grafo; }
        }, new LugaresNodosRepository() {
            @Override public boolean existe() { return existe; }
            @Override public List<FilaLugarNodo> leer() { return filas; }
        }, new CatalogoService(null) {
            @Override public List<Lugar> lugares() {
                return List.of(new Lugar("a", "A", "m", "cabecera", 4.5, -75.7, null),
                        new Lugar("b", "B", "m", "atractivo", 4.7, -75.5, 60),
                        new Lugar("c", "C", "m", "cabecera", 4.5, -75.7, null));
            }
        });
    }

    @Test void t5ra_caminoDirigidoYCoordenadas() {
        var s = servicio(grafo(), asociaciones(), true);
        var ruta = s.calcular("a", "b");
        assertEquals(5, ruta.tiempoS());
        assertEquals(3, ruta.coordenadas().size());
        assertArrayEquals(new double[] {4.6, -75.6}, ruta.coordenadas().get(1));
        assertEquals("a", ruta.idOrigen());
        assertEquals("b", ruta.idDestino());
        assertEquals(9, s.calcular("b", "a").tiempoS());
    }

    @Test void t5rb_mismoNodo() {
        var s = servicio(grafo(), asociaciones(), true);
        for (String destino : List.of("a", "c")) {
            var ruta = s.calcular("a", destino);
            assertEquals(0, ruta.tiempoS());
            assertEquals(1, ruta.coordenadas().size());
            assertEquals(1, ruta.nodosExplorados());
        }
    }

    @Test void t5rc_idsInvalidos() {
        var s = servicio(grafo(), asociaciones(), true);
        for (String id : new String[] {null, "", " ", "otro"}) {
            assertThrows(IllegalArgumentException.class, () -> s.calcular(id, "b"));
            assertThrows(IllegalArgumentException.class, () -> s.calcular("a", id));
        }
    }

    @Test void t5rd_datosInvalidos() {
        var fueraDeRango = Grafo.desdeArreglos(3, new int[] {0}, new int[] {2}, new double[] {1},
                new double[] {1}, null, null, null, new double[] {91, 4, 4}, new double[] {-75, -75, -75});
        assertThrows(IllegalStateException.class,
                () -> servicio(fueraDeRango, asociaciones(), true).calcular("a", "b"));
        assertThrows(IllegalStateException.class,
                () -> servicio(Grafo.dirigido(3, new double[][] {{0, 2, 1}}), asociaciones(), true).calcular("a", "b"));
        assertThrows(IllegalStateException.class, () -> servicio(grafo(), asociaciones(), false).calcular("a", "b"));
        for (var filas : List.of(List.of(new FilaLugarNodo("a", 0, 0, false, "")),
                List.of(new FilaLugarNodo("a", 0, 0, false, ""), new FilaLugarNodo("a", 1, 0, false, "")),
                List.of(new FilaLugarNodo("a", 3, 0, false, ""), new FilaLugarNodo("b", 2, 0, false, "")))) {
            assertThrows(IllegalStateException.class, () -> servicio(grafo(), filas, true).calcular("a", "b"));
        }
    }

    @Test void t5re_inalcanzable() {
        assertThrows(IllegalStateException.class,
                () -> servicio(Grafo.dirigido(3, new double[0][]), asociaciones(), true).calcular("a", "b"));
    }

    @Test void t5rf_cacheConcurrenteCargaUnaVez() throws Exception {
        var lecturas = new AtomicInteger();
        var red = new RedService(new RedRepository() {
            @Override public boolean existe() { return true; }
            @Override public RedLeida leer() {
                lecturas.incrementAndGet();
                return new RedLeida(1, new int[0], new int[0], new double[0], new double[0],
                        new long[] {1}, new double[] {0}, new double[] {0}, new double[] {4.5}, new double[] {-75.7});
            }
        });
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var a = ejecutor.submit(red::grafo);
            var b = ejecutor.submit(red::grafo);
            assertSame(a.get(), b.get());
            assertEquals(1, lecturas.get());
        }
    }
}
