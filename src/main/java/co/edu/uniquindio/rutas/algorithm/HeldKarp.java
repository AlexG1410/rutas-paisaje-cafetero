package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.ResultadoTsp;

/**
 * R4.1 — Held-Karp: programación dinámica sobre subconjuntos (spec: specs/R4-ruta-optima.md).
 * IMPLEMENTACIÓN PROPIA. Convención de entrada y salida: ver {@link Tsp}.
 *
 * <p>Estado dp[S][j]: costo mínimo de salir de la partida, visitar exactamente los lugares del
 * subconjunto S (máscara de bits sobre los k lugares) y terminar en j ∈ S. Debe devolver el
 * RECORRIDO (tabla de predecesores), no solo el costo.
 */
public final class HeldKarp {

    private HeldKarp() {
    }

    /**
     * Recorrido óptimo con Held-Karp.
     *
     * <p>Tiempo Θ(k² · 2^k), memoria Θ(k · 2^k). Fuente: Held y Karp (1962) [verificar cita].
     */
    public static ResultadoTsp resolver(double[][] matriz, int[] lugares) {
        throw new UnsupportedOperationException("Pendiente R4.1 — ver specs/R4-ruta-optima.md");
    }

    /** Número de celdas de la tabla dp para k lugares, k · 2^k (para estimar memoria). */
    public static long entradasTabla(int k) {
        throw new UnsupportedOperationException("Pendiente R4.1 — ver specs/R4-ruta-optima.md");
    }
}
