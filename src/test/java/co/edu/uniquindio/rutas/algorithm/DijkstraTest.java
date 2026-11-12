package co.edu.uniquindio.rutas.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.uniquindio.rutas.model.Arista;
import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.model.ResultadoDijkstra;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Casos T2-* de specs/R2-caminos-minimos.md. Cada caso se prueba en las dos versiones. */
class DijkstraTest {

    private static final double TOL = 1e-6;

    /** Grafo de la guía 08 (nodos 1..7 del curso -> 0..6 en código), no dirigido. */
    private static Grafo grafoGuia() {
        int[][] aristas = {{1, 2, 1}, {2, 3, 2}, {4, 5, 3}, {6, 7, 3}, {1, 4, 4}, {2, 5, 4},
            {4, 7, 4}, {3, 5, 5}, {2, 4, 6}, {3, 6, 6}, {5, 7, 7}, {5, 6, 8}};
        double[][] filas = new double[aristas.length][];
        for (int i = 0; i < aristas.length; i++) {
            filas[i] = new double[] {aristas[i][0] - 1, aristas[i][1] - 1, aristas[i][2]};
        }
        return Grafo.noDirigido(7, filas);
    }

    private record Version(String nombre, Ejecutor ejecutor) {
    }

    private interface Ejecutor {
        ResultadoDijkstra ejecutar(Grafo g, int origen, int destino);
    }

    private static final List<Version> VERSIONES = List.of(
            new Version("lineal", Dijkstra::lineal),
            new Version("cola_prioridad", Dijkstra::colaPrioridad));

    private static int[] mas1(int[] ruta) {
        return Arrays.stream(ruta).map(v -> v + 1).toArray();
    }

    @Test
    void t2a_distanciasDeLaGuia() {
        ResultadoDijkstra r = Dijkstra.colaPrioridad(grafoGuia(), 0, Dijkstra.SIN_DESTINO);
        assertArrayEquals(new double[] {0, 1, 3, 4, 5, 9, 8}, r.distancias(), TOL);
    }

    @Test
    void t2b_ruta1a6() {
        for (Version v : VERSIONES) {
            ResultadoDijkstra r = v.ejecutor().ejecutar(grafoGuia(), 0, 5);
            assertEquals(9.0, r.tiempoS(), TOL, v.nombre());
            assertArrayEquals(new int[] {1, 2, 3, 6}, mas1(r.ruta()), v.nombre());
        }
    }

    @Test
    void t2c_ruta1a7() {
        for (Version v : VERSIONES) {
            ResultadoDijkstra r = v.ejecutor().ejecutar(grafoGuia(), 0, 6);
            assertEquals(8.0, r.tiempoS(), TOL, v.nombre());
            assertArrayEquals(new int[] {1, 4, 7}, mas1(r.ruta()), v.nombre());
        }
    }

    @Test
    void t2d_asimetria() {
        Grafo g = Grafo.dirigido(2, new double[][] {{0, 1, 1.0}, {1, 0, 5.0}});
        for (Version v : VERSIONES) {
            assertEquals(1.0, v.ejecutor().ejecutar(g, 0, 1).tiempoS(), TOL, v.nombre());
            assertEquals(5.0, v.ejecutor().ejecutar(g, 1, 0).tiempoS(), TOL, v.nombre());
        }
    }

    @Test
    void t2e_inalcanzable() {
        Grafo g = Grafo.dirigido(2, new double[0][]);
        for (Version v : VERSIONES) {
            ResultadoDijkstra r = v.ejecutor().ejecutar(g, 0, 1);
            assertTrue(Double.isInfinite(r.tiempoS()), v.nombre());
            assertEquals(0, r.ruta().length, v.nombre());
            assertEquals(1, r.nodosExplorados(), v.nombre());
        }
    }

    @Test
    void t2f_origenIgualDestino() {
        for (Version v : VERSIONES) {
            ResultadoDijkstra r = v.ejecutor().ejecutar(grafoGuia(), 3, 3);
            assertEquals(0.0, r.tiempoS(), TOL, v.nombre());
            assertArrayEquals(new int[] {3}, r.ruta(), v.nombre());
            assertEquals(1, r.nodosExplorados(), v.nombre());
        }
    }

    @Test
    void t2g_paradaTemprana() {
        Grafo g = Grafo.dirigido(5, new double[][] {{0, 1, 1}, {1, 2, 1}, {2, 3, 1}, {3, 4, 1}});
        for (Version v : VERSIONES) {
            ResultadoDijkstra r = v.ejecutor().ejecutar(g, 0, 1);
            assertEquals(1.0, r.tiempoS(), TOL, v.nombre());
            assertEquals(2, r.nodosExplorados(), v.nombre());
        }
    }

    /** T2-h: 30 grafos dirigidos al azar contra Bellman-Ford (algoritmo independiente). */
    @Test
    void t2h_contraBellmanFord() {
        Random azar = new Random(2026);
        for (int caso = 0; caso < 30; caso++) {
            int n = 2 + azar.nextInt(59);
            Grafo g = grafoAlAzar(azar, n, n + azar.nextInt(3 * n + 1));
            int origen = azar.nextInt(n);
            int destino = azar.nextInt(n);
            double[] esperado = bellmanFord(g, origen);
            for (Version v : VERSIONES) {
                ResultadoDijkstra r = v.ejecutor().ejecutar(g, origen, destino);
                assertEquals(esperado[destino], r.tiempoS(), TOL, v.nombre() + " caso " + caso);
                if (r.ruta().length > 0) {
                    assertEquals(origen, r.ruta()[0]);
                    assertEquals(destino, r.ruta()[r.ruta().length - 1]);
                    assertEquals(esperado[destino], costoRuta(g, r.ruta()), TOL);
                }
            }
        }
    }

    @Test
    void t2h_distanciasCompletasContraBellmanFord() {
        Random azar = new Random(7);
        Grafo g = grafoAlAzar(azar, 80, 320);
        double[] esperado = bellmanFord(g, 0);
        double[] obtenido = Dijkstra.colaPrioridad(g, 0, Dijkstra.SIN_DESTINO).distancias();
        for (int v = 0; v < 80; v++) {
            if (Double.isInfinite(esperado[v])) {
                assertTrue(Double.isInfinite(obtenido[v]));
            } else {
                assertEquals(esperado[v], obtenido[v], TOL);
            }
        }
    }

    // --- utilidades de prueba ---

    private static Grafo grafoAlAzar(Random azar, int n, int m) {
        double[][] filas = new double[m][];
        int k = 0;
        for (int i = 0; i < m; i++) {
            int u = azar.nextInt(n);
            int v = azar.nextInt(n);
            double w = 1 + azar.nextDouble() * 99;
            if (u != v) {
                filas[k++] = new double[] {u, v, w};
            }
        }
        return Grafo.dirigido(n, Arrays.copyOf(filas, k));
    }

    /** Oráculo: Bellman-Ford O(V·E), sin cola de prioridad. */
    private static double[] bellmanFord(Grafo g, int origen) {
        double[] dist = new double[g.n()];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        dist[origen] = 0;
        for (int i = 0; i < g.n() - 1; i++) {
            for (int u = 0; u < g.n(); u++) {
                if (Double.isInfinite(dist[u])) {
                    continue;
                }
                for (Arista a : g.vecinos(u)) {
                    dist[a.destino()] = Math.min(dist[a.destino()], dist[u] + a.tiempoS());
                }
            }
        }
        return dist;
    }

    private static double costoRuta(Grafo g, int[] ruta) {
        double total = 0;
        for (int i = 0; i + 1 < ruta.length; i++) {
            int destino = ruta[i + 1];
            total += Arrays.stream(g.vecinos(ruta[i])).filter(a -> a.destino() == destino)
                    .mapToDouble(Arista::tiempoS).min().orElseThrow();
        }
        return total;
    }

}
