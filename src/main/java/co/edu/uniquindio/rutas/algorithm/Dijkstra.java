package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.model.ResultadoDijkstra;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

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
        if (predecesor == null || origen < 0 || destino < 0
                || origen >= predecesor.length || destino >= predecesor.length) {
            throw new IllegalArgumentException("Origen, destino o predecesores inválidos");
        }
        ArrayList<Integer> inversa = new ArrayList<>();
        int actual = destino;
        while (actual != origen) {
            if (actual == -1) {
                return new int[0];
            }
            if (actual < 0 || actual >= predecesor.length || inversa.size() >= predecesor.length) {
                throw new IllegalArgumentException("Cadena de predecesores inválida o cíclica");
            }
            inversa.add(actual);
            actual = predecesor[actual];
        }
        inversa.add(origen);
        int[] ruta = new int[inversa.size()];
        for (int i = 0; i < ruta.length; i++) {
            ruta[i] = inversa.get(ruta.length - 1 - i);
        }
        return ruta;
    }

    /**
     * Dijkstra buscando el mínimo con un recorrido lineal del arreglo dist (sin cola).
     * Se detiene al fijar {@code destino}.
     *
     * <p>Tiempo O(V² + E) = O(V²) sobre el grafo deduplicado, memoria O(V).
     * Derivación por conteo en docs/documento-tecnico/03-r2-caminos-minimos.md;
     * idea/correctitud: Sedgewick y Wayne, Algorithms, §4.4 (algs4.cs.princeton.edu/44sp/).
     */
    public static ResultadoDijkstra lineal(Grafo grafo, int origen, int destino) {
        validar(grafo, origen, destino);
        double[] dist = distanciasIniciales(grafo.n(), origen);
        int[] previo = new int[grafo.n()];
        Arrays.fill(previo, -1);
        boolean[] fijado = new boolean[grafo.n()];
        int explorados = 0;
        while (true) {
            int u = -1;
            for (int v = 0; v < grafo.n(); v++) {
                if (!fijado[v] && Double.isFinite(dist[v]) && (u == -1 || dist[v] < dist[u])) {
                    u = v; // Recorrido ascendente: empate por menor índice.
                }
            }
            if (u == -1) {
                break;
            }
            fijado[u] = true;
            explorados++;
            if (u == destino) {
                break;
            }
            for (var arista : grafo.vecinos(u)) {
                int v = arista.destino();
                if (!fijado[v]) {
                    double candidata = sumar(dist[u], arista.tiempoS());
                    if (candidata < dist[v]) {
                        dist[v] = candidata;
                        previo[v] = u;
                    }
                }
            }
        }
        return resultado(dist, previo, origen, destino, explorados);
    }

    /**
     * Dijkstra con cola de prioridad ({@code java.util.PriorityQueue}, montículo binario) y
     * borrado perezoso. Si {@code destino != SIN_DESTINO}, se detiene al sacarlo de la cola
     * por primera vez; si es {@code SIN_DESTINO}, calcula {@code distancias} a todos.
     *
     * <p>Tiempo O(V + E log(E + 1)), acotado por O((V + E) log V) para V >= 2;
     * memoria O(V + E). Derivación del borrado perezoso en el documento R2; Oracle,
     * Javadoc Java SE 21 de PriorityQueue: offer/poll O(log n). Idea: Sedgewick y Wayne, §4.4.
     */
    public static ResultadoDijkstra colaPrioridad(Grafo grafo, int origen, int destino) {
        validar(grafo, origen, destino);
        double[] dist = distanciasIniciales(grafo.n(), origen);
        int[] previo = new int[grafo.n()];
        Arrays.fill(previo, -1);
        boolean[] fijado = new boolean[grafo.n()];
        PriorityQueue<Entrada> cola = new PriorityQueue<>();
        cola.offer(new Entrada(0, origen));
        int explorados = 0;
        while (!cola.isEmpty()) {
            Entrada entrada = cola.poll();
            int u = entrada.nodo();
            if (fijado[u]) {
                continue; // Entrada obsoleta: no vuelve a explorar ni a contar el nodo.
            }
            fijado[u] = true;
            explorados++;
            if (u == destino) {
                break;
            }
            for (var arista : grafo.vecinos(u)) {
                int v = arista.destino();
                if (!fijado[v]) {
                    double candidata = sumar(dist[u], arista.tiempoS());
                    if (candidata < dist[v]) {
                        dist[v] = candidata;
                        previo[v] = u;
                        cola.offer(new Entrada(candidata, v));
                    }
                }
            }
        }
        return resultado(dist, previo, origen, destino, explorados);
    }

    private record Entrada(double distanciaS, int nodo) implements Comparable<Entrada> {
        @Override public int compareTo(Entrada otra) {
            int comparacion = Double.compare(distanciaS, otra.distanciaS);
            return comparacion != 0 ? comparacion : Integer.compare(nodo, otra.nodo);
        }
    }

    private static void validar(Grafo grafo, int origen, int destino) {
        if (grafo == null || origen < 0 || origen >= grafo.n()
                || destino < SIN_DESTINO || destino >= grafo.n()) {
            throw new IllegalArgumentException("Grafo, origen o destino inválidos");
        }
    }

    private static double[] distanciasIniciales(int n, int origen) {
        double[] dist = new double[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        dist[origen] = 0;
        return dist;
    }

    private static double sumar(double distanciaS, double pesoS) {
        double suma = distanciaS + pesoS;
        if (!Double.isFinite(suma)) {
            throw new IllegalArgumentException("El costo del camino desborda double");
        }
        return suma;
    }

    private static ResultadoDijkstra resultado(double[] dist, int[] previo, int origen,
                                                int destino, int explorados) {
        double tiempoS = destino == SIN_DESTINO ? Double.POSITIVE_INFINITY : dist[destino];
        int[] ruta = Double.isFinite(tiempoS) ? reconstruirRuta(previo, origen, destino) : new int[0];
        return new ResultadoDijkstra(tiempoS, ruta, explorados, dist);
    }
}
