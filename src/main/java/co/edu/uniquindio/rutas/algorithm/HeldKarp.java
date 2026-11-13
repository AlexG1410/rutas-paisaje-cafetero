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
     * <p>dp[S,j]=min_i(dp[S\{j},i]+M[i,j]); base dp[{j},j]=M[p,j].
     * Tiempo Θ(k² · 2^k) más validación O(N+k²), memoria Θ(k · 2^k), incluida salida.
     * Fuente: guía 09 del curso, sección 9 (Held-Karp); derivación en capítulo R4 del
     * documento técnico. Máscaras sobre posiciones locales, con k<=20 por recursos.
     */
    public static ResultadoTsp resolver(double[][] matriz, int[] lugares) {
        Tsp.validar(matriz, lugares);
        int k = lugares.length - 1;
        if (k > 20) throw new IllegalArgumentException("Held-Karp admite como máximo 20 visitas");
        if (k == 0) return new ResultadoTsp(0, new int[] {lugares[0], lugares[0]});
        // El bit j representa lugares[j+1]. Una máscara tiene k bits de visitas, sin partida.
        int subconjuntos = 1 << k;
        double[] dp = new double[(int) entradasTabla(k)];
        int[] anterior = new int[dp.length];
        java.util.Arrays.fill(dp, Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(anterior, -1);
        for (int j = 0; j < k; j++) dp[(1 << j) * k + j] = matriz[lugares[0]][lugares[j + 1]];
        for (int conjunto = 1; conjunto < subconjuntos; conjunto++) {
            for (int j = 0; j < k; j++) {
                if ((conjunto & (1 << j)) == 0) continue;
                int sinJ = conjunto ^ (1 << j); // Quitar el bit j, cuya pertenencia ya se comprobó.
                if (sinJ == 0) continue;
                int celda = conjunto * k + j;
                for (int i = 0; i < k; i++) {
                    if ((sinJ & (1 << i)) == 0) continue;
                    double candidato = Tsp.sumar(dp[sinJ * k + i], matriz[lugares[i + 1]][lugares[j + 1]]);
                    if (candidato < dp[celda]) {
                        dp[celda] = candidato;
                        anterior[celda] = i;
                    }
                }
            }
        }
        int completo = subconjuntos - 1;
        double mejorS = Double.POSITIVE_INFINITY;
        int ultimo = -1;
        for (int j = 0; j < k; j++) {
            double costoS = Tsp.sumar(dp[completo * k + j], matriz[lugares[j + 1]][lugares[0]]);
            if (costoS < mejorS) {
                mejorS = costoS;
                ultimo = j;
            }
        }
        int[] recorrido = new int[k + 2];
        recorrido[0] = recorrido[k + 1] = lugares[0];
        int conjunto = completo;
        for (int posicion = k; posicion >= 1; posicion--) {
            recorrido[posicion] = lugares[ultimo + 1];
            int previo = anterior[conjunto * k + ultimo];
            conjunto ^= 1 << ultimo;
            ultimo = previo;
        }
        return new ResultadoTsp(mejorS, recorrido);
    }

    /** Número de celdas de la tabla dp para k lugares, k · 2^k (para estimar memoria). */
    public static long entradasTabla(int k) {
        if (k < 0 || k > 57) throw new IllegalArgumentException("k debe estar entre 0 y 57");
        return k * (1L << k);
    }
}
