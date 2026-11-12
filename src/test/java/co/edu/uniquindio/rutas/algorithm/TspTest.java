package co.edu.uniquindio.rutas.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uniquindio.rutas.model.ResultadoTsp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Casos T4-* de specs/R4-ruta-optima.md. */
class TspTest {

    private static final double TOL = 1e-9;

    private interface Metodo {
        ResultadoTsp resolver(double[][] matriz, int[] lugares);
    }

    private record Nombrado(String nombre, Metodo metodo) {
    }

    private static final List<Nombrado> EXACTOS = List.of(
            new Nombrado("fuerza_bruta", TspFuerzaBruta::resolver),
            new Nombrado("held_karp", HeldKarp::resolver));

    /** Matriz A, B, C, D de la guía 09 (óptimo 80). */
    private static final double[][] MATRIZ_GUIA = {
        {0, 10, 15, 20}, {10, 0, 35, 25}, {15, 35, 0, 30}, {20, 25, 30, 0}};
    /** El vecino más cercano da 23 y el óptimo es 18 (verificado a mano y por fuerza bruta). */
    private static final double[][] MATRIZ_VORAZ_FALLA = {
        {0, 7, 8, 6}, {7, 0, 9, 2}, {8, 9, 0, 1}, {6, 2, 1, 0}};
    private static final double[][] MATRIZ_ASIMETRICA = {{0, 1, 9}, {9, 0, 1}, {1, 9, 0}};

    @Test
    void t4a_ejemploGuia09() {
        int[] lugares = {0, 1, 2, 3};
        for (Nombrado m : EXACTOS) {
            ResultadoTsp r = m.metodo().resolver(MATRIZ_GUIA, lugares);
            assertEquals(80.0, r.costoS(), TOL, m.nombre());
            assertTrue(Tsp.esRecorridoValido(r.recorrido(), lugares), m.nombre());
            assertEquals(80.0, Tsp.costoRecorrido(MATRIZ_GUIA, r.recorrido()), TOL, m.nombre());
        }
    }

    @Test
    void t4b_vecinoMasCercanoNoEsOptimo() {
        int[] lugares = {0, 1, 2, 3};
        ResultadoTsp voraz = VecinoMasCercano.resolver(MATRIZ_VORAZ_FALLA, lugares);
        assertEquals(23.0, voraz.costoS(), TOL);
        assertArrayEquals(new int[] {0, 3, 2, 1, 0}, voraz.recorrido());
        assertEquals(18.0, HeldKarp.resolver(MATRIZ_VORAZ_FALLA, lugares).costoS(), TOL);
        assertEquals(18.0, TspFuerzaBruta.resolver(MATRIZ_VORAZ_FALLA, lugares).costoS(), TOL);
    }

    @Test
    void t4c_asimetrica() {
        for (Nombrado m : EXACTOS) {
            ResultadoTsp r = m.metodo().resolver(MATRIZ_ASIMETRICA, new int[] {0, 1, 2});
            assertEquals(3.0, r.costoS(), TOL, m.nombre());
            assertArrayEquals(new int[] {0, 1, 2, 0}, r.recorrido(), m.nombre());
        }
    }

    @Test
    void t4d_bordes() {
        List<Nombrado> todos = new ArrayList<>(EXACTOS);
        todos.add(new Nombrado("voraz", VecinoMasCercano::resolver));
        for (Nombrado m : todos) {
            ResultadoTsp k0 = m.metodo().resolver(MATRIZ_GUIA, new int[] {2});
            assertEquals(0.0, k0.costoS(), TOL, m.nombre());
            assertArrayEquals(new int[] {2, 2}, k0.recorrido(), m.nombre());
            ResultadoTsp k1 = m.metodo().resolver(MATRIZ_ASIMETRICA, new int[] {0, 1});
            assertEquals(10.0, k1.costoS(), TOL, m.nombre());
            assertArrayEquals(new int[] {0, 1, 0}, k1.recorrido(), m.nombre());
        }
    }

    @Test
    void t4e_lugaresNoConsecutivos() {
        double[][] m = matrizAlAzar(new Random(5), 8);
        int[] lugares = {5, 2, 7, 0};
        ResultadoTsp bf = TspFuerzaBruta.resolver(m, lugares);
        ResultadoTsp hk = HeldKarp.resolver(m, lugares);
        assertEquals(bf.costoS(), hk.costoS(), TOL);
        assertTrue(Tsp.esRecorridoValido(hk.recorrido(), lugares));
    }

    @Test
    void t4f_t4h_verificacionCruzada() {
        Random azar = new Random(2026);
        for (int caso = 0; caso < 40; caso++) {
            int k = 1 + azar.nextInt(7);
            double[][] m = matrizAlAzar(azar, 10);
            int[] lugares = muestra(azar, 10, k + 1);
            ResultadoTsp bf = TspFuerzaBruta.resolver(m, lugares);
            ResultadoTsp hk = HeldKarp.resolver(m, lugares);
            ResultadoTsp voraz = VecinoMasCercano.resolver(m, lugares);
            assertEquals(bf.costoS(), hk.costoS(), TOL, "caso " + caso);
            for (ResultadoTsp r : List.of(bf, hk, voraz)) {
                assertTrue(Tsp.esRecorridoValido(r.recorrido(), lugares), "caso " + caso);
                assertEquals(r.costoS(), Tsp.costoRecorrido(m, r.recorrido()), TOL);
            }
            assertTrue(voraz.costoS() >= hk.costoS() - TOL, "la heurística no puede ganarle al óptimo");
        }
    }

    @Test
    void t4g_entradasTabla() {
        for (int k = 0; k <= 12; k++) {
            assertEquals((long) k << k, HeldKarp.entradasTabla(k));
        }
    }

    @Test
    void utilidades_costoYValidez() {
        assertEquals(80.0, Tsp.costoRecorrido(MATRIZ_GUIA, new int[] {0, 2, 3, 1, 0}), TOL);
        assertTrue(Tsp.esRecorridoValido(new int[] {0, 2, 3, 1, 0}, new int[] {0, 1, 2, 3}));
        assertTrue(!Tsp.esRecorridoValido(new int[] {0, 2, 2, 1, 0}, new int[] {0, 1, 2, 3}));
        assertTrue(!Tsp.esRecorridoValido(new int[] {1, 2, 3, 0, 1}, new int[] {0, 1, 2, 3}));
    }

    // --- utilidades de prueba ---

    private static double[][] matrizAlAzar(Random azar, int n) {
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                m[i][j] = i == j ? 0 : 60 + azar.nextDouble() * 3540;
            }
        }
        return m;
    }

    private static int[] muestra(Random azar, int n, int tamano) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, azar);
        return indices.subList(0, tamano).stream().mapToInt(Integer::intValue).toArray();
    }
}
