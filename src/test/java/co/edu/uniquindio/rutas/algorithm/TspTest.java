package co.edu.uniquindio.rutas.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.uniquindio.rutas.model.ResultadoTsp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Casos T4-* de specs/R4-ruta-optima.md. */
class TspTest {

    private List<Nombrado> todos() {
        var todos = new ArrayList<>(EXACTOS);
        todos.add(new Nombrado("voraz", VecinoMasCercano::resolver));
        return todos;
    }

    @Test void t4i_entradasInvalidas() {
        for (var m : todos()) {
            assertThrows(IllegalArgumentException.class, () -> m.metodo().resolver(null, new int[] {0}));
            assertThrows(IllegalArgumentException.class, () -> m.metodo().resolver(MATRIZ_GUIA, null));
            for (int[] lugares : List.of(new int[0], new int[] {0, 0}, new int[] {-1}, new int[] {4})) {
                assertThrows(IllegalArgumentException.class, () -> m.metodo().resolver(MATRIZ_GUIA, lugares));
            }
            assertThrows(IllegalArgumentException.class,
                    () -> m.metodo().resolver(new double[][] {{0, 1}, {1}}, new int[] {0}));
            for (double malo : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
                assertThrows(IllegalArgumentException.class,
                        () -> m.metodo().resolver(new double[][] {{0, malo}, {1, 0}}, new int[] {0, 1}));
            }
            assertThrows(IllegalArgumentException.class, () -> m.metodo().resolver(
                    new double[][] {{0, Double.MAX_VALUE}, {Double.MAX_VALUE, 0}}, new int[] {0, 1}));
        }
        for (int k : new int[] {-1, 58, 64}) {
            assertThrows(IllegalArgumentException.class, () -> HeldKarp.entradasTabla(k));
        }
        assertEquals(57L * (1L << 57), HeldKarp.entradasTabla(57));
        assertThrows(IllegalArgumentException.class,
                () -> HeldKarp.resolver(new double[22][22], java.util.stream.IntStream.range(0, 22).toArray()));
    }

    @Test void t4j_empatesCerosYEntradaIntacta() {
        double[][] matriz = new double[4][4];
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) matriz[i][j] = i == j ? 0 : 1;
        int[] lugares = {2, 3, 0, 1};
        assertArrayEquals(new int[] {2, 3, 0, 1, 2}, TspFuerzaBruta.resolver(matriz, lugares).recorrido());
        assertArrayEquals(new int[] {2, 3, 0, 1, 2}, VecinoMasCercano.resolver(matriz, lugares).recorrido());
        assertArrayEquals(new int[] {2, 1, 0, 3, 2}, HeldKarp.resolver(matriz, lugares).recorrido());
        for (var m : todos()) {
            assertEquals(4, m.metodo().resolver(matriz, lugares).costoS());
            assertEquals(0, m.metodo().resolver(new double[4][4], lugares).costoS());
        }
        assertArrayEquals(new int[] {2, 3, 0, 1}, lugares);
        assertArrayEquals(new double[] {0, 1, 1, 1}, matriz[0]);
    }

    @Test void t4k_oraculoEnumeracionSinIntercambios() {
        Random azar = new Random(2026);
        for (int caso = 0; caso < 15; caso++) {
            double[][] matriz = matrizAlAzar(azar, 7);
            int[] lugares = muestra(azar, 7, 6);
            double esperado = enumerar(matriz, lugares, new boolean[6], 1, lugares[0], 0);
            for (var m : EXACTOS) assertEquals(esperado, m.metodo().resolver(matriz, lugares).costoS(), 1e-8);
        }
    }

    private double enumerar(double[][] matriz, int[] lugares, boolean[] usados, int nivel, int actual, double costo) {
        if (nivel == lugares.length) return costo + matriz[actual][lugares[0]];
        double mejor = Double.POSITIVE_INFINITY;
        for (int i = 1; i < lugares.length; i++) if (!usados[i]) {
            usados[i] = true;
            mejor = Math.min(mejor, enumerar(matriz, lugares, usados, nivel + 1, lugares[i],
                    costo + matriz[actual][lugares[i]]));
            usados[i] = false;
        }
        return mejor;
    }

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
        double[][] m = matrizAlAzar(new Random(2026), 8);
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
