package co.edu.uniquindio.rutas.model;

/** Held-Karp frente a la heurística para la misma selección (R5.3). */
public record ComparacionItinerario(Itinerario optimo, Itinerario heuristico,
                                    double diferenciaMin, double diferenciaPct) {
}
