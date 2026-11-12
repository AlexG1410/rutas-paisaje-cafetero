package co.edu.uniquindio.rutas.service;

import static org.junit.jupiter.api.Assertions.*;

import co.edu.uniquindio.rutas.algorithm.EnvolventeConvexa;
import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.model.Punto;
import co.edu.uniquindio.rutas.repository.Csv;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import co.edu.uniquindio.rutas.service.measurement.Medicion;
import co.edu.uniquindio.rutas.service.measurement.Medidor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExperimentoR3ServiceTest {
    @TempDir Path carpeta;

    private Grafo red() {
        return Grafo.desdeArreglos(6, new int[0], new int[0], new double[0], new double[0],
                null, new double[] {0, 10, 10, 0, 5, 0.2}, new double[] {0, 0, 10, 10, 5, 0.2},
                new double[] {4, 4.1, 4.2, 4.3, 4.4, 4.5}, new double[] {-75, -75, -75, -75, -75, -75});
    }

    private class ExperimentoPrueba extends ExperimentoR3Service {
        int llamadas;
        boolean timeout;
        boolean incorrecto;
        ExperimentoPrueba() {
            super(new RedService(null) { @Override public Grafo grafo() { return red(); } },
                    new ResultadosRepository(carpeta));
        }
        @Override Medicion medir(ArrayList<Punto> puntos) {
            llamadas++;
            if (timeout) {
                return Medicion.noCompletada(List.of());
            }
            return new Medicion(List.of(1.0), true,
                    incorrecto ? List.of(new Punto(99, 99)) : EnvolventeConvexa.calcular(puntos));
        }
    }

    @Test void t3ia_conjuntosExportacionYMapaReproducibles() throws Exception {
        var e = new ExperimentoPrueba();
        e.ejecutar();
        assertEquals(4, e.llamadas);
        var tabla = Csv.leer(carpeta.resolve("r3/envolvente_propio.csv"));
        assertEquals(List.of("red", "cuadrado_1000", "cuadrado_10000", "cuadrado_50000"),
                tabla.stream().map(f -> f.get("conjunto")).toList());
        assertEquals("5", tabla.getFirst().get("n"));
        assertEquals("4", tabla.getFirst().get("vertices"));
        for (var fila : tabla) {
            var puntos = Csv.leer(carpeta.resolve("r3/puntos_" + fila.get("conjunto") + ".csv"));
            assertEquals(Integer.parseInt(fila.get("n")), puntos.size());
            assertEquals(puntos.size(), puntos.stream().distinct().count());
            assertEquals("2026", fila.get("semilla"));
        }
        var mapa = Csv.leer(carpeta.resolve("r3/envolvente_red.csv"));
        assertEquals(4, mapa.size());
        assertEquals("0", mapa.getFirst().get("idx_nodo"));
        assertEquals("4.0", mapa.getFirst().get("lat"));
        byte[] anterior = Files.readAllBytes(carpeta.resolve("r3/puntos_cuadrado_50000.csv"));
        e.ejecutar();
        assertArrayEquals(anterior, Files.readAllBytes(carpeta.resolve("r3/puntos_cuadrado_50000.csv")));
    }

    @Test void t3ib_errorNoSobrescribeYTimeoutEsExplicito() throws Exception {
        var e = new ExperimentoPrueba();
        Path archivo = carpeta.resolve("r3/envolvente_propio.csv");
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, "anterior");
        e.incorrecto = true;
        assertThrows(IllegalStateException.class, e::ejecutar);
        assertEquals("anterior", Files.readString(archivo));
        e.incorrecto = false;
        e.timeout = true;
        e.ejecutar();
        assertTrue(Csv.leer(archivo).stream().allMatch(f -> f.get("estado").equals("no completada")
                && f.get("tiempo_propio_ms").isEmpty()));
    }

    @Test void t3k_redondeoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> ExperimentoR3Service.redondearYDeduplicar(null, new double[0]));
        assertThrows(IllegalArgumentException.class,
                () -> ExperimentoR3Service.redondearYDeduplicar(new double[1], new double[0]));
        for (double valor : new double[] {Double.NaN, Double.POSITIVE_INFINITY, 0x1.0p63}) {
            assertThrows(IllegalArgumentException.class,
                    () -> ExperimentoR3Service.redondearYDeduplicar(new double[] {valor}, new double[] {0}));
        }
        assertEquals(List.of(new Punto(-1, 0)), ExperimentoR3Service.redondearYDeduplicar(
                new double[] {-1.5, -1.49}, new double[] {0, 0}));
    }

    @Test void t3ia_tareaSerializableReal() {
        var entrada = new ArrayList<>(List.of(new Punto(0, 0), new Punto(1, 0), new Punto(0, 1)));
        var m = Medidor.medirConLimite(TareaEnvolvente.class, entrada, 1, 0, 60, 30);
        assertTrue(m.completada());
        assertEquals(entrada, m.resultado());
    }

    @Test void t3ib_redSinCoordenadasNoMideNiPublica() {
        var e = new ExperimentoR3Service(new RedService(null) {
            @Override public Grafo grafo() { return Grafo.dirigido(2, new double[0][]); }
        }, new ResultadosRepository(carpeta)) {
            @Override Medicion medir(ArrayList<Punto> puntos) {
                fail("No debe medir una red inválida");
                return null;
            }
        };
        assertThrows(IllegalStateException.class, e::ejecutar);
        assertFalse(Files.exists(carpeta.resolve("r3/envolvente_propio.csv")));
    }
}
