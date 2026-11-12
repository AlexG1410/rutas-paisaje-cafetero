package co.edu.uniquindio.rutas.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.uniquindio.rutas.model.Punto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Casos T3-* de specs/R3-envolvente-convexa.md. La verificación contra
 * scipy.spatial.ConvexHull se hace en python/ (comando envolvente-scipy); aquí el oráculo de
 * los casos al azar es la cadena monótona de Andrew, un algoritmo distinto e independiente.
 */
class EnvolventeConvexaTest {

    @Test void t3i_duplicadosEmpatesYEntradaIntacta() {
        var entrada = new ArrayList<>(List.of(p(0, 0), p(4, 0), p(4, 4), p(0, 4),
                p(2, 0), p(2, 4), p(0, 4), p(0, 0)));
        var copia = List.copyOf(entrada);
        var esperado = List.of(p(0, 0), p(4, 0), p(4, 4), p(0, 4));
        assertEquals(esperado, EnvolventeConvexa.calcular(entrada));
        assertEquals(copia, entrada);
        Collections.reverse(entrada);
        assertEquals(esperado, EnvolventeConvexa.calcular(entrada));
        assertEquals(List.of(p(1, -2), p(1, 8)),
                EnvolventeConvexa.calcular(List.of(p(1, 3), p(1, 8), p(1, -2))));
        assertEquals(List.of(p(3, 4)), EnvolventeConvexa.calcular(List.of(p(3, 4), p(3, 4))));
    }

    @Test void t3j_exactitudYDesbordamiento() {
        assertEquals(1_000_000_000_000L, EnvolventeConvexa.orientacion(
                p(400_000, 400_000), p(1_400_000, 400_000), p(400_000, 1_400_000)));
        assertThrows(IllegalArgumentException.class, () -> EnvolventeConvexa.orientacion(
                p(Long.MIN_VALUE, 0), p(Long.MAX_VALUE, 0), p(0, 1)));
        assertThrows(IllegalArgumentException.class, () -> EnvolventeConvexa.orientacion(
                p(0, 0), p(4_000_000_000L, 0), p(0, 4_000_000_000L)));
    }

    @Test void t3k_rechazaNull() {
        assertThrows(IllegalArgumentException.class, () -> EnvolventeConvexa.calcular(null));
        assertThrows(IllegalArgumentException.class, () -> EnvolventeConvexa.calcular(
                java.util.Arrays.asList(p(0, 0), null)));
        assertThrows(IllegalArgumentException.class,
                () -> EnvolventeConvexa.orientacion(null, p(0, 0), p(1, 1)));
    }

    @Test void t3i_multiplesNubesConRepetidosContraOraculo() {
        Random azar = new Random(2026);
        for (int caso = 0; caso < 40; caso++) {
            List<Punto> puntos = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                puntos.add(p(azar.nextInt(21) - 10, azar.nextInt(21) - 10));
            }
            verificarContraOraculo(puntos);
        }
    }

    private static Punto p(long x, long y) {
        return new Punto(x, y);
    }

    @Test
    void t3a_orientacion() {
        assertTrue(EnvolventeConvexa.orientacion(p(0, 0), p(1, 0), p(0, 1)) > 0);
        assertTrue(EnvolventeConvexa.orientacion(p(0, 0), p(0, 1), p(1, 0)) < 0);
        assertEquals(0, EnvolventeConvexa.orientacion(p(0, 0), p(1, 1), p(2, 2)));
    }

    @Test
    void t3b_cuadradoConColinealesEInteriores() {
        List<Punto> puntos = List.of(p(2, 2), p(4, 2), p(0, 4), p(2, 0), p(4, 4), p(1, 3),
                p(0, 0), p(4, 0));
        assertEquals(List.of(p(0, 0), p(4, 0), p(4, 4), p(0, 4)),
                EnvolventeConvexa.calcular(puntos));
    }

    @Test
    void t3c_todosColineales() {
        assertEquals(List.of(p(0, 0), p(3, 3)),
                EnvolventeConvexa.calcular(List.of(p(2, 2), p(0, 0), p(3, 3), p(1, 1))));
    }

    @Test
    void t3d_casosBorde() {
        assertEquals(List.of(), EnvolventeConvexa.calcular(List.of()));
        assertEquals(List.of(p(1, 2)), EnvolventeConvexa.calcular(List.of(p(1, 2))));
    }

    @Test
    void t3e_trianguloConInteriores() {
        Random azar = new Random(2026);
        Set<Punto> interiores = new LinkedHashSet<>();
        while (interiores.size() < 20) {
            Punto q = p(1 + azar.nextInt(9), 1 + azar.nextInt(7));
            if (orient(p(0, 0), p(10, 0), q) > 0 && orient(p(10, 0), p(5, 8), q) > 0
                    && orient(p(5, 8), p(0, 0), q) > 0) {
                interiores.add(q);
            }
        }
        List<Punto> puntos = new ArrayList<>(interiores);
        puntos.addAll(List.of(p(5, 8), p(0, 0), p(10, 0)));
        Collections.shuffle(puntos, azar);
        assertEquals(List.of(p(0, 0), p(10, 0), p(5, 8)), EnvolventeConvexa.calcular(puntos));
    }

    @Test
    void t3f_cuadradoAlAzarContraOraculo() {
        Random azar = new Random(2026);
        Set<Punto> unicos = new LinkedHashSet<>();
        for (int i = 0; i < 2000; i++) {
            unicos.add(p(azar.nextInt(1_000_000), azar.nextInt(1_000_000)));
        }
        verificarContraOraculo(new ArrayList<>(unicos));
    }

    @Test
    void t3g_cercaDeUnaCircunferenciaContraOraculo() {
        Random azar = new Random(2026);
        Set<Punto> unicos = new LinkedHashSet<>();
        for (int i = 0; i < 500; i++) {
            double t = azar.nextDouble() * 2 * Math.PI;
            unicos.add(p(Math.round(100_000 * Math.cos(t)), Math.round(100_000 * Math.sin(t))));
        }
        verificarContraOraculo(new ArrayList<>(unicos));
    }

    private static void verificarContraOraculo(List<Punto> puntos) {
        List<Punto> propio = EnvolventeConvexa.calcular(puntos);
        assertEquals(new HashSet<>(cadenaMonotona(puntos)), new HashSet<>(propio));
        assertEquals(propio.size(), new HashSet<>(propio).size(), "vértices repetidos");
        assertEquals(Collections.min(propio), propio.getFirst(), "debe empezar por el menor (x, y)");
        int n = propio.size();
        for (int i = 0; i < n; i++) {
            assertTrue(orient(propio.get(i), propio.get((i + 1) % n), propio.get((i + 2) % n)) > 0,
                    "debe ser antihorario y sin colineales");
        }
    }

    // --- oráculo independiente (solo para pruebas) ---

    private static long orient(Punto a, Punto b, Punto c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    /** Cadena monótona de Andrew: envolvente sin colineales, O(n log n). */
    private static List<Punto> cadenaMonotona(List<Punto> entrada) {
        List<Punto> pts = new ArrayList<>(new HashSet<>(entrada));
        Collections.sort(pts);
        if (pts.size() < 3) {
            return pts;
        }
        List<Punto> casco = new ArrayList<>();
        for (int fase = 0; fase < 2; fase++) {
            int inicio = casco.size();
            for (Punto q : pts) {
                while (casco.size() >= inicio + 2
                        && orient(casco.get(casco.size() - 2), casco.getLast(), q) <= 0) {
                    casco.removeLast();
                }
                casco.add(q);
            }
            casco.removeLast();
            Collections.reverse(pts);
        }
        return casco;
    }
}
