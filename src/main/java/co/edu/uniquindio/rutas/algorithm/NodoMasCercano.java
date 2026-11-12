package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.NodoCercano;

/**
 * R1.4 — Nodo más cercano en línea recta (spec: specs/R1-modelo-red.md). IMPLEMENTACIÓN
 * PROPIA: recorrido lineal de todos los nodos de la componente. La verificación contra
 * {@code ox.distance.nearest_nodes} se hace en python/ (comando verificar-lugares).
 *
 * <p>Tiempo Θ(V) y memoria auxiliar Θ(1): se calcula una distancia por nodo y se conserva
 * solo el mejor índice y su distancia. Derivación propia en la sección 2.5 del documento
 * técnico, bajo el modelo de costo constante para operaciones con {@code double}.
 * Fuente de la operación de distancia: Oracle, Javadoc de Java SE 21, {@link Math#hypot}.
 */
public final class NodoMasCercano {

    private NodoMasCercano() {
    }

    /**
     * Nodo más cercano a (x, y) con distancia euclidiana, coordenadas en metros (UTM 18N).
     * En empate gana el índice menor. Los arreglos de nodos corresponden a la misma red
     * no vacía y tienen igual longitud. Tiempo Θ(V), memoria auxiliar Θ(1).
     */
    public static NodoCercano buscar(double[] xs, double[] ys, double x, double y) {
        int indiceMejor = 0;
        double distanciaMejorM = Math.hypot(xs[0] - x, ys[0] - y);

        for (int i = 1; i < xs.length; i++) {
            double distanciaM = Math.hypot(xs[i] - x, ys[i] - y);
            // El orden creciente y la comparación estricta conservan el menor índice en empate.
            if (distanciaM < distanciaMejorM) {
                indiceMejor = i;
                distanciaMejorM = distanciaM;
            }
        }

        return new NodoCercano(indiceMejor, distanciaMejorM);
    }
}
