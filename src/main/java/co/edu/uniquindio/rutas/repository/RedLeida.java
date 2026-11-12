package co.edu.uniquindio.rutas.repository;

/**
 * Red procesada en arreglos planos, tal como está en data/processed. La capa de datos no
 * conoce el {@code Grafo}: la capa lógica lo construye con estos arreglos (regla de capas).
 *
 * @param n     número de nodos (índices 0..n-1)
 * @param u     origen de cada tramo
 * @param v     destino de cada tramo
 */
public record RedLeida(int n, int[] u, int[] v, double[] tiempoS, double[] longitudM,
                       long[] osmid, double[] lat, double[] lon, double[] x, double[] y) {
}
