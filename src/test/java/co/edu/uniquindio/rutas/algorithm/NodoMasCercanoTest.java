package co.edu.uniquindio.rutas.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.edu.uniquindio.rutas.model.NodoCercano;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Casos T1.4-* de specs/R1-modelo-red.md (la verificación con OSMnx está en python/). */
class NodoMasCercanoTest {

    private static final double[] XS = {0, 10, 0};
    private static final double[] YS = {0, 0, 10};

    @Test
    void t1_4a_casoBasico() {
        NodoCercano r = NodoMasCercano.buscar(XS, YS, 6, 1);
        assertEquals(1, r.indice());
        assertEquals(Math.sqrt(17), r.distanciaM(), 1e-9);
    }

    @Test
    void t1_4b_empateGanaElMenorIndice() {
        NodoCercano r = NodoMasCercano.buscar(XS, YS, 5, 0);
        assertEquals(0, r.indice());
        assertEquals(5.0, r.distanciaM(), 1e-9);
    }

    @Test
    void t1_4c_unSoloNodo() {
        NodoCercano r = NodoMasCercano.buscar(new double[] {3}, new double[] {4}, 0, 0);
        assertEquals(0, r.indice());
        assertEquals(5.0, r.distanciaM(), 1e-9);
    }

    @Test
    void t1_4d_contraBusquedaDirecta() {
        Random azar = new Random(2026);
        double[] xs = new double[500];
        double[] ys = new double[500];
        for (int i = 0; i < 500; i++) {
            xs[i] = azar.nextDouble() * 10_000;
            ys[i] = azar.nextDouble() * 10_000;
        }
        for (int caso = 0; caso < 50; caso++) {
            double x = azar.nextDouble() * 10_000;
            double y = azar.nextDouble() * 10_000;
            int esperado = 0;
            for (int i = 1; i < 500; i++) {
                if (Math.hypot(xs[i] - x, ys[i] - y) < Math.hypot(xs[esperado] - x, ys[esperado] - y)) {
                    esperado = i;
                }
            }
            NodoCercano r = NodoMasCercano.buscar(xs, ys, x, y);
            assertEquals(esperado, r.indice());
            assertEquals(Math.hypot(xs[esperado] - x, ys[esperado] - y), r.distanciaM(), 1e-9);
        }
    }

    @Test
    void t1_4e_coincidenciaExacta() {
        NodoCercano r = NodoMasCercano.buscar(XS, YS, 0, 10);
        assertEquals(2, r.indice());
        assertEquals(0.0, r.distanciaM());
    }

    @Test
    void t1_4f_empateEntreNodosPosteriores() {
        NodoCercano r = NodoMasCercano.buscar(
                new double[] {100, 0, 10}, new double[] {100, 0, 0}, 5, 0);
        assertEquals(1, r.indice());
        assertEquals(5.0, r.distanciaM(), 1e-9);
    }
}
