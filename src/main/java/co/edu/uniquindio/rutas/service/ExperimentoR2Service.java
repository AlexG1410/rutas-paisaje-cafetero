package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.LugaresNodosRepository;
import co.edu.uniquindio.rutas.repository.MatrizRepository;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import org.springframework.stereotype.Service;

/**
 * R2 — Experimentos de Dijkstra y matriz de tiempos (spec: specs/R2-caminos-minimos.md).
 *
 * <p>Pasos de {@link #ejecutar()}:
 * <ol>
 *   <li>Elegir ≥ 20 pares (10 cercanos para la versión lineal) → results/r2/pares.csv.</li>
 *   <li>VERIFICAR (6.6): en los 10 pares, las dos versiones dan el mismo tiempo (1e-6 s)
 *       → results/r2/verificacion.csv; abortar si no.</li>
 *   <li>Medir con {@code Medidor.medir} → results/r2/mediciones_dijkstra.csv
 *       (origen, destino, version, tiempo_viaje_s, nodos_explorados, tiempo_ms,
 *       repeticiones, estado).</li>
 *   <li>Cociente lineal / cola en los 10 pares → results/r2/cociente.csv (mediana, minimo,
 *       maximo).</li>
 *   <li>Matriz de tiempos con {@code Dijkstra.colaPrioridad(..., SIN_DESTINO)} desde cada
 *       lugar → results/r2/matriz_tiempos.csv ({@link MatrizRepository}).</li>
 * </ol>
 * Después: {@code python -m preparacion verificar-dijkstra} compara los pares con NetworkX.
 */
@Service
public class ExperimentoR2Service {

    private final RedService redService;
    private final CatalogoService catalogoService;
    private final LugaresNodosRepository lugaresNodos;
    private final MatrizRepository matriz;
    private final ResultadosRepository resultados;

    public ExperimentoR2Service(RedService redService, CatalogoService catalogoService,
                                LugaresNodosRepository lugaresNodos, MatrizRepository matriz,
                                ResultadosRepository resultados) {
        this.redService = redService;
        this.catalogoService = catalogoService;
        this.lugaresNodos = lugaresNodos;
        this.matriz = matriz;
        this.resultados = resultados;
    }

    public void ejecutar() {
        throw new UnsupportedOperationException("Pendiente R2 — ver specs/R2-caminos-minimos.md");
    }
}
