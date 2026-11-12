package co.edu.uniquindio.rutas.model;

import java.util.List;

/**
 * Itinerario de un día (R4.4 y R5.3), con el formato de la tabla del anexo.
 *
 * @param metodo      "held_karp" o el nombre de la heurística
 * @param paradas     en orden; la última es el regreso a la partida
 * @param retirados   atractivos retirados para cumplir la jornada (con el criterio de la spec)
 */
public record Itinerario(String metodo, String idPartida, List<Parada> paradas,
                         double totalVisitasMin, double totalDesplazamientosMin,
                         double duracionJornadaMin, List<String> retirados) {

    /** Fila de la tabla: orden, lugar, desplazamiento desde el anterior, visita, acumulado. */
    public record Parada(int orden, String idLugar, String nombre, double desplazamientoMin,
                         double visitaMin, double tiempoAcumuladoH) {
    }
}
