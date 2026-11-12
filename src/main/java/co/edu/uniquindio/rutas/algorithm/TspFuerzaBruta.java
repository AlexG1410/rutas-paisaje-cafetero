package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.ResultadoTsp;

/**
 * R4.1 — Viajante asimétrico por fuerza bruta (spec: specs/R4-ruta-optima.md).
 * IMPLEMENTACIÓN PROPIA: genera las k! permutaciones con un generador propio.
 * Convención de entrada y salida: ver {@link Tsp}.
 */
public final class TspFuerzaBruta {

    private TspFuerzaBruta() {
    }

    /**
     * Recorrido óptimo revisando todas las permutaciones de lugares[1..k]. En empate conserva
     * la primera encontrada.
     *
     * <p>Tiempo Θ(k · k!) (o Θ(k!) si el costo se acumula de forma incremental; documentar cuál
     * se implementó). Memoria O(k). Fuente: [citar en el documento técnico].
     */
    public static ResultadoTsp resolver(double[][] matriz, int[] lugares) {
        throw new UnsupportedOperationException("Pendiente R4.1 — ver specs/R4-ruta-optima.md");
    }
}
