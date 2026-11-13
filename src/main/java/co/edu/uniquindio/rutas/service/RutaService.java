package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.LugaresNodosRepository;
import co.edu.uniquindio.rutas.model.RutaCalculada;
import co.edu.uniquindio.rutas.algorithm.Dijkstra;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final CatalogoService catalogoService;

    public RutaService(RedService redService, LugaresNodosRepository lugaresNodos,
                       CatalogoService catalogoService) {
        this.redService = redService;
        this.lugaresNodos = lugaresNodos;
        this.catalogoService = catalogoService;
    }

    /**
     * Busca el nodo de cada lugar (data/processed/lugares_nodos.csv), ejecuta Dijkstra con
     * cola de prioridad y convierte la ruta de nodos a coordenadas [lat, lon].
     */
    public RutaCalculada calcular(String idOrigen, String idDestino) {
        if (idOrigen == null || idDestino == null || idOrigen.isBlank() || idDestino.isBlank()) {
            throw new IllegalArgumentException("Seleccione un origen y un destino");
        }
        var catalogo = catalogoService.lugares();
        if (catalogo.stream().noneMatch(l -> l.id().equals(idOrigen))
                || catalogo.stream().noneMatch(l -> l.id().equals(idDestino))) {
            throw new IllegalArgumentException("Origen o destino desconocido en el catálogo");
        }
        if (!lugaresNodos.existe()) {
            throw new IllegalStateException("Falta lugares_nodos.csv. Ejecutar ./gradlew bootRun --args='r1'");
        }
        var grafo = redService.grafo();
        var nodos = new HashMap<String, Integer>();
        try {
            for (var fila : lugaresNodos.leer()) {
                if (fila.idLugar() == null || fila.idLugar().isBlank() || fila.idxNodo() < 0
                        || fila.idxNodo() >= grafo.n() || nodos.putIfAbsent(fila.idLugar(), fila.idxNodo()) != null) {
                    throw new IllegalStateException("Asociaciones inválidas. Regenerar con ./gradlew bootRun --args='r1'");
                }
            }
        } catch (IllegalArgumentException | UncheckedIOException e) {
            throw new IllegalStateException("No se pudo leer lugares_nodos.csv. Ejecutar ./gradlew bootRun --args='r1'", e);
        }
        if (!nodos.containsKey(idOrigen) || !nodos.containsKey(idDestino)) {
            throw new IllegalStateException("Falta asociación de origen o destino. Ejecutar ./gradlew bootRun --args='r1'");
        }
        var resultado = Dijkstra.colaPrioridad(grafo, nodos.get(idOrigen), nodos.get(idDestino));
        double tiempoS = resultado.tiempoS();
        if (!Double.isFinite(tiempoS) || resultado.ruta().length == 0) {
            throw new IllegalStateException("No hay camino dirigido entre los nodos de estos lugares; revisar la red procesada");
        }
        var coordenadas = new ArrayList<double[]>();
        for (int nodo : resultado.ruta()) {
            double lat;
            double lon;
            try {
                lat = grafo.lat(nodo);
                lon = grafo.lon(nodo);
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalStateException("La red no tiene coordenadas; ejecutar python -m preparacion procesar", e);
            }
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                throw new IllegalStateException("La red no tiene coordenadas geográficas válidas; ejecutar python -m preparacion procesar");
            }
            coordenadas.add(new double[] {lat, lon});
        }
        return new RutaCalculada(idOrigen, idDestino, tiempoS, resultado.nodosExplorados(),
                java.util.List.copyOf(coordenadas));
    }
}
