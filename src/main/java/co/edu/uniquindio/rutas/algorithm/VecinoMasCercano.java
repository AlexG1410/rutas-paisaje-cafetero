package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.ResultadoTsp;

/**
 * R4.2 — Heurística voraz para el viajante asimétrico (spec: specs/R4-ruta-optima.md).
 * IMPLEMENTACIÓN PROPIA. Convención de entrada y salida: ver {@link Tsp}.
 *
 * <p>Decisión inicial del equipo (modificable, justificar en el documento): vecino más
 * cercano. Elementos voraces: candidatos = lugares no visitados; selección = el de menor
 * matriz[actual][j]; factibilidad = no visitado; solución = todos visitados + regreso.
 */
public final class VecinoMasCercano {

    private VecinoMasCercano() {
    }

    /**
     * Desde el lugar actual, ir al no visitado más cercano; al final, regresar a la partida.
     * Empates: gana el que aparece primero en {@code lugares}. Tiempo Θ(k²), memoria O(k).
     */
    public static ResultadoTsp resolver(double[][] matriz, int[] lugares) {
        throw new UnsupportedOperationException("Pendiente R4.2 — ver specs/R4-ruta-optima.md");
    }
}
