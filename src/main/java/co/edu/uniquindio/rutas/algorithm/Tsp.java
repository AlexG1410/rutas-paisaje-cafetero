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
            total = sumar(total, matriz[recorrido[i]][recorrido[i + 1]]);
        }
        return total;
    }

    /** True si sale y vuelve a lugares[0] y visita cada otro lugar exactamente una vez. */
    public static boolean esRecorridoValido(int[] recorrido, int[] lugares) {
        if (recorrido == null || lugares == null || lugares.length == 0) {
            return false;
        }
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

    /** Forma O(N), selección y pesos O(k²); sin copiar la matriz general. */
    static void validar(double[][] matriz, int[] lugares) {
        if (matriz == null || matriz.length == 0 || lugares == null || lugares.length == 0) {
            throw new IllegalArgumentException("Matriz y lugares deben incluir la partida");
        }
        for (double[] fila : matriz) {
            if (fila == null || fila.length != matriz.length) {
                throw new IllegalArgumentException("La matriz debe ser cuadrada");
            }
        }
        for (int i = 0; i < lugares.length; i++) {
            int u = lugares[i];
            if (u < 0 || u >= matriz.length) throw new IllegalArgumentException("Índice fuera de matriz");
            for (int j = 0; j < i; j++) {
                if (u == lugares[j]) throw new IllegalArgumentException("Lugares repetidos");
            }
        }
        for (int u : lugares) for (int v : lugares) {
            double valor = matriz[u][v];
            if (!Double.isFinite(valor) || valor < 0 || (u == v && valor != 0)) {
                throw new IllegalArgumentException("Pesos finitos no negativos y diagonal cero requeridos");
            }
        }
    }

    static double sumar(double a, double b) {
        double suma = a + b;
        if (!Double.isFinite(suma)) throw new IllegalArgumentException("El costo desborda double finito");
        return suma;
    }
}
