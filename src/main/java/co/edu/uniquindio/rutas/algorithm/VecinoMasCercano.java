package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.ResultadoTsp;

/**
 * R4.2 — Heurística voraz para el viajante asimétrico (spec: specs/R4-ruta-optima.md).
 * IMPLEMENTACIÓN PROPIA. Convención de entrada y salida: ver {@link Tsp}.
 *
 * <p>Decisión del equipo: vecino más
 * cercano. Elementos voraces: candidatos = lugares no visitados; selección = el de menor
 * matriz[actual][j]; factibilidad = no visitado; solución = todos visitados + regreso.
 */
public final class VecinoMasCercano {

    private VecinoMasCercano() {
    }

    /**
     * Desde el lugar actual, ir al no visitado más cercano; al final, regresar a la partida.
     * Empates: gana el que aparece primero en {@code lugares}. Tiempo Θ(k²), más validación
     * O(N+k²); memoria O(k). Derivación propia de barridos en capítulo R4; elementos
     * voraces según guía 08 del curso, sin garantía de optimalidad para TSP.
     */
    public static ResultadoTsp resolver(double[][] matriz, int[] lugares) {
        Tsp.validar(matriz, lugares);
        int[] recorrido = new int[lugares.length + 1];
        boolean[] visitado = new boolean[lugares.length];
        recorrido[0] = lugares[0];
        visitado[0] = true;
        for (int paso = 1; paso < lugares.length; paso++) {
            int elegido = -1;
            double menorS = Double.POSITIVE_INFINITY;
            for (int j = 1; j < lugares.length; j++) {
                if (!visitado[j] && matriz[recorrido[paso - 1]][lugares[j]] < menorS) {
                    elegido = j;
                    menorS = matriz[recorrido[paso - 1]][lugares[j]];
                }
            }
            recorrido[paso] = lugares[elegido];
            visitado[elegido] = true;
        }
        recorrido[recorrido.length - 1] = lugares[0];
        return new ResultadoTsp(Tsp.costoRecorrido(matriz, recorrido), recorrido);
    }
}
