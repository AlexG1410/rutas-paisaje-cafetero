package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.model.ResultadoDijkstra;

/**
 * R2.1 — Dijkstra en dos versiones (spec: specs/R2-caminos-minimos.md). IMPLEMENTACIÓN PROPIA.
 * Peso: tiempo de viaje en segundos ({@code Arista.tiempoS()}).
 */
public final class Dijkstra {

    /** Valor de {@code destino} para calcular las distancias a todos los nodos (matriz R2). */
    public static final int SIN_DESTINO = -1;

    private Dijkstra() {
    }

    /**
     * Reconstruye la ruta origen -> destino siguiendo {@code predecesor} hacia atrás.
     * {@code predecesor[v] == -1} indica que v no tiene predecesor. Devuelve un arreglo
     * vacío si el destino no es alcanzable.
     *
     * <p>Tiempo O(longitud de la ruta), memoria O(longitud de la ruta).
     */
    public static int[] reconstruirRuta(int[] predecesor, int origen, int destino) {
        throw new UnsupportedOperationException("Pendiente R2.1 — ver specs/R2-caminos-minimos.md");
    }

    /**
     * Dijkstra buscando el mínimo con un recorrido lineal del arreglo dist (sin cola).
     * Se detiene al fijar {@code destino}.
     *
     * <p>Tiempo O(V²), memoria O(V). Fuente: [citar en el documento técnico].
     */
    public static ResultadoDijkstra lineal(Grafo grafo, int origen, int destino) {
        throw new UnsupportedOperationException("Pendiente R2.1 — ver specs/R2-caminos-minimos.md");
    }

    /**
     * Dijkstra con cola de prioridad ({@code java.util.PriorityQueue}, montículo binario) y
     * borrado perezoso. Si {@code destino != SIN_DESTINO}, se detiene al sacarlo de la cola
     * por primera vez; si es {@code SIN_DESTINO}, calcula {@code distancias} a todos.
     *
     * <p>Tiempo O((V + E) log V), memoria O(V + E). Fuente: [citar en el documento técnico].
     */
    public static ResultadoDijkstra colaPrioridad(Grafo grafo, int origen, int destino) {
        throw new UnsupportedOperationException("Pendiente R2.1 — ver specs/R2-caminos-minimos.md");
    }
}
