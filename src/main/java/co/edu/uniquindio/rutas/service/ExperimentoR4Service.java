package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.MatrizRepository;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import org.springframework.stereotype.Service;

/**
 * R4 — Experimentos del viajante asimétrico (spec: specs/R4-ruta-optima.md).
 *
 * <p>Pasos de {@link #ejecutar()}:
 * <ol>
 *   <li>Instancias: partida Armenia + k lugares al azar (semilla derivada de
 *       {@code Constantes.SEMILLA}) → results/r4/instancias.csv.</li>
 *   <li>VERIFICAR (6.6): fuerza bruta = Held-Karp en cada k que la fuerza bruta resuelva.</li>
 *   <li>Exactas k = 5..12 con {@code Medidor.medirConLimite(TareasTsp.FuerzaBruta.class, ...)}
 *       y {@code TareasTsp.HeldKarpTarea} → results/r4/exactas.csv. Al primer k "no
 *       completada" de un método, no se prueban k mayores para ese método.</li>
 *   <li>Memoria de Held-Karp por k → results/r4/memoria_held_karp.csv.</li>
 *   <li>Calidad de la heurística (k = 5, 8, 10, 12; ≥ 5 instancias) →
 *       results/r4/calidad_heuristica.csv.</li>
 *   <li>Itinerario de un día ({@link ItinerarioService#planear}) → results/r4/itinerario.csv
 *       y results/r4/itinerario_resumen.csv.</li>
 *   <li>Contraejemplo voraz vs óptimo → results/r4/voraz_vs_optimo.csv.</li>
 * </ol>
 */
@Service
public class ExperimentoR4Service {

    private final MatrizRepository matriz;
    private final CatalogoService catalogoService;
    private final ItinerarioService itinerarioService;
    private final ResultadosRepository resultados;

    public ExperimentoR4Service(MatrizRepository matriz, CatalogoService catalogoService,
                                ItinerarioService itinerarioService,
                                ResultadosRepository resultados) {
        this.matriz = matriz;
        this.catalogoService = catalogoService;
        this.itinerarioService = itinerarioService;
        this.resultados = resultados;
    }

    public void ejecutar() {
        throw new UnsupportedOperationException("Pendiente R4 — ver specs/R4-ruta-optima.md");
    }
}
