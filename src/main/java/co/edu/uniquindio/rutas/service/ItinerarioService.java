package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.MatrizRepository;
import co.edu.uniquindio.rutas.model.ComparacionItinerario;
import co.edu.uniquindio.rutas.model.Itinerario;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * R4.4 y R5.3 — Itinerario de un día (spec: specs/R4-ruta-optima.md, sección R4.4).
 *
 * <p>Ordena los atractivos con Held-Karp sobre la matriz de tiempos de R2, suma
 * desplazamientos y visitas, y si la jornada pasa de {@code Constantes.JORNADA_MIN} retira
 * atractivos con el criterio documentado en la spec y recalcula.
 */
@Service
public class ItinerarioService {

    private final MatrizRepository matrizRepository;
    private final CatalogoService catalogoService;

    public ItinerarioService(MatrizRepository matrizRepository, CatalogoService catalogoService) {
        this.matrizRepository = matrizRepository;
        this.catalogoService = catalogoService;
    }

    /** Itinerario óptimo (Held-Karp) con retiro de atractivos si no cabe en 11 h. */
    public Itinerario planear(String idPartida, List<String> idsAtractivos) {
        throw new UnsupportedOperationException("Pendiente R4.4 — ver specs/R4-ruta-optima.md");
    }

    /** Held-Karp frente a la heurística, SIN retirar atractivos (R5.3). */
    public ComparacionItinerario comparar(String idPartida, List<String> idsAtractivos) {
        throw new UnsupportedOperationException("Pendiente R5.3 — ver specs/R5-aplicacion.md");
    }
}
