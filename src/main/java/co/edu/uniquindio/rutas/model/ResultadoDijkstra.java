package co.edu.uniquindio.rutas.model;

/**
 * Salida común de las dos versiones de Dijkstra (R2.1).
 *
 * @param tiempoS         tiempo mínimo origen -> destino en segundos;
 *                        {@code Double.POSITIVE_INFINITY} si no hay camino o si no hubo destino
 * @param ruta            índices de nodo desde origen hasta destino, inclusive (vacío si no
 *                        hay camino o si no hubo destino)
 * @param nodosExplorados nodos fijados (sacados de la cola, o elegidos en el recorrido lineal)
 *                        por PRIMERA vez, incluido el destino
 * @param distancias      arreglo dist[0..V-1] al terminar (lo usa la matriz de tiempos)
 */
public record ResultadoDijkstra(double tiempoS, int[] ruta, int nodosExplorados,
                                double[] distancias) implements java.io.Serializable {
}
