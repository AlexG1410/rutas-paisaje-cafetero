package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.LugaresNodosRepository;
import co.edu.uniquindio.rutas.model.RutaCalculada;
import org.springframework.stereotype.Service;

/**
 * R5.1 — Ruta entre dos lugares del catálogo, calculada EN EL MOMENTO con
 * {@code Dijkstra.colaPrioridad} sobre el grafo de {@link RedService}
 * (spec: specs/R5-aplicacion.md).
 */
@Service
public class RutaService {

    private final RedService redService;
    private final LugaresNodosRepository lugaresNodos;

    public RutaService(RedService redService, LugaresNodosRepository lugaresNodos) {
        this.redService = redService;
        this.lugaresNodos = lugaresNodos;
    }

    /**
     * Busca el nodo de cada lugar (data/processed/lugares_nodos.csv), ejecuta Dijkstra con
     * cola de prioridad y convierte la ruta de nodos a coordenadas [lat, lon].
     */
    public RutaCalculada calcular(String idOrigen, String idDestino) {
        throw new UnsupportedOperationException("Pendiente R5.1 — ver specs/R5-aplicacion.md");
    }
}
