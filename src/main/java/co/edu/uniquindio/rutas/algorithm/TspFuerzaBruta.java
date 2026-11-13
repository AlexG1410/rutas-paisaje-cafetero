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
     * <p>Tiempo Θ(k · k!) calculando cada costo completo; validación O(N+k²).
     * Memoria O(k) con copia, resultado y pila. Recurrencia de generación
     * G(k)=k·G(k−1)+O(k); k! hojas de costo Θ(k). Derivación propia en el capítulo R4
     * del documento técnico; guía 09, sección TSP, para la enumeración de recorridos.
     */
    public static ResultadoTsp resolver(double[][] matriz, int[] lugares) {
        Tsp.validar(matriz, lugares);
        int[] recorrido = java.util.Arrays.copyOf(lugares, lugares.length + 1);
        recorrido[recorrido.length - 1] = lugares[0];
        Mejor mejor = new Mejor();
        permutar(matriz, recorrido, 1, mejor);
        return new ResultadoTsp(mejor.costoS, mejor.recorrido);
    }

    private static final class Mejor {
        private double costoS = Double.POSITIVE_INFINITY;
        private int[] recorrido;
    }

    private static void permutar(double[][] matriz, int[] recorrido, int posicion, Mejor mejor) {
        if (posicion == recorrido.length - 1) {
            double costoS = Tsp.costoRecorrido(matriz, recorrido);
            if (costoS < mejor.costoS) {
                mejor.costoS = costoS;
                mejor.recorrido = recorrido.clone();
            }
            return;
        }
        for (int i = posicion; i < recorrido.length - 1; i++) {
            intercambiar(recorrido, posicion, i);
            permutar(matriz, recorrido, posicion + 1, mejor);
            intercambiar(recorrido, posicion, i);
        }
    }

    private static void intercambiar(int[] valores, int a, int b) {
        int temporal = valores[a];
        valores[a] = valores[b];
        valores[b] = temporal;
    }
}
