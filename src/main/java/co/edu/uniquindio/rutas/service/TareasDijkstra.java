package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.algorithm.Dijkstra;
import co.edu.uniquindio.rutas.model.ResultadoDijkstra;
import co.edu.uniquindio.rutas.service.measurement.Tarea;

/** Adaptadores R2: entrada ya preparada, solo se cronometra algoritmo y reconstrucción. */
public final class TareasDijkstra {
    private TareasDijkstra() {
    }

    public static final class Lineal implements Tarea<InstanciaDijkstra, ResultadoDijkstra> {
        @Override public ResultadoDijkstra ejecutar(InstanciaDijkstra entrada) {
            return Dijkstra.lineal(entrada.grafo(), entrada.origen(), entrada.destino());
        }
    }

    public static final class Cola implements Tarea<InstanciaDijkstra, ResultadoDijkstra> {
        @Override public ResultadoDijkstra ejecutar(InstanciaDijkstra entrada) {
            return Dijkstra.colaPrioridad(entrada.grafo(), entrada.origen(), entrada.destino());
        }
    }
}
