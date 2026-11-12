package co.edu.uniquindio.rutas.algorithm;

import java.util.Arrays;

/**
 * Utilidades comunes del viajante asimétrico (R4).
 *
 * <p>Convención de entrada para TODOS los métodos de R4:
 * <ul>
 *   <li>{@code matriz[i][j]}: segundos de i a j entre lugares del catálogo. No es simétrica.</li>
 *   <li>{@code lugares}: índices de fila/columna de la matriz. {@code lugares[0]} es la
 *       partida; {@code lugares[1..k]} son los k lugares a visitar.</li>
 * </ul>
 * Salida: {@code recorrido} empieza y termina en {@code lugares[0]} y contiene cada lugar a
 * visitar exactamente una vez (longitud k + 2).
 */
public final class Tsp {

    private Tsp() {
    }

    /** Suma de matriz[a][b] para cada par consecutivo del recorrido. Tiempo O(longitud). */
    public static double costoRecorrido(double[][] matriz, int[] recorrido) {
        double total = 0.0;
        for (int i = 0; i + 1 < recorrido.length; i++) {
            total += matriz[recorrido[i]][recorrido[i + 1]];
        }
        return total;
    }

    /** True si sale y vuelve a lugares[0] y visita cada otro lugar exactamente una vez. */
    public static boolean esRecorridoValido(int[] recorrido, int[] lugares) {
        if (recorrido.length != lugares.length + 1) {
            return false;
        }
        if (recorrido[0] != lugares[0] || recorrido[recorrido.length - 1] != lugares[0]) {
            return false;
        }
        int[] visitados = Arrays.copyOfRange(recorrido, 1, recorrido.length - 1);
        int[] esperados = Arrays.copyOfRange(lugares, 1, lugares.length);
        Arrays.sort(visitados);
        Arrays.sort(esperados);
        return Arrays.equals(visitados, esperados);
    }
}
