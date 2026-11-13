package co.edu.uniquindio.rutas.controller;

import co.edu.uniquindio.rutas.Constantes;
import co.edu.uniquindio.rutas.model.ComparacionItinerario;
import co.edu.uniquindio.rutas.service.ItinerarioService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** R5.3 — Itinerario: Held-Karp frente a la heurística. */
@RestController
@RequestMapping("/api")
public class ItinerarioController {

    /** Cuerpo de POST /api/itinerario. {@code partida} vacía = Armenia. */
    public record SolicitudItinerario(String partida, List<String> atractivos) {
    }

    private final ItinerarioService itinerarioService;

    public ItinerarioController(ItinerarioService itinerarioService) {
        this.itinerarioService = itinerarioService;
    }

    @PostMapping("/itinerario")
    public ComparacionItinerario itinerario(@RequestBody SolicitudItinerario solicitud) {
        if (solicitud == null) throw new IllegalArgumentException("Indique partida y entre 6 y 8 atractivos");
        List<String> atractivos = solicitud.atractivos() == null ? List.of() : solicitud.atractivos();
        if (atractivos.size() < Constantes.ATRACTIVOS_MIN
                || atractivos.size() > Constantes.ATRACTIVOS_MAX) {
            throw new IllegalArgumentException("Elegir entre " + Constantes.ATRACTIVOS_MIN
                    + " y " + Constantes.ATRACTIVOS_MAX + " atractivos");
        }
        String partida = solicitud.partida() == null || solicitud.partida().isBlank()
                ? Constantes.ID_PARTIDA_DEFECTO : solicitud.partida();
        return itinerarioService.comparar(partida, atractivos);
    }
}
