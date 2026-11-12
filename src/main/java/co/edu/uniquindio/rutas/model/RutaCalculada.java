package co.edu.uniquindio.rutas.model;

import java.util.List;

/**
 * Ruta origen–destino para la aplicación (R5.1).
 *
 * @param coordenadas  [lat, lon] de cada nodo de la ruta, en orden
 */
public record RutaCalculada(String idOrigen, String idDestino, double tiempoS,
                            int nodosExplorados, List<double[]> coordenadas) {
}
