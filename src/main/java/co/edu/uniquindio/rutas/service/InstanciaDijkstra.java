package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.model.Grafo;
import java.io.Serializable;

/** Entrada preparada antes del cronómetro; transporte local hacia la JVM de medición R2. */
public record InstanciaDijkstra(Grafo grafo, int origen, int destino) implements Serializable {
}
