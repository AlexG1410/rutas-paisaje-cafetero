package co.edu.uniquindio.rutas.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grafo dirigido ponderado con LISTAS DE ADYACENCIA (spec R1.1). Implementación propia.
 *
 * <p>Los nodos se reindexan a enteros contiguos 0..V-1 para usar arreglos en los algoritmos;
 * el identificador de OpenStreetMap se conserva en {@link #osmid(int)}.
 *
 * <p>Memoria: O(V + E). Recorrer los vecinos de u cuesta O(grado de salida de u).
 */
public final class Grafo {

    private final int n;
    private final Arista[][] adyacencia;
    private final long[] osmid;
    private final double[] x; // UTM 18N (m)
    private final double[] y; // UTM 18N (m)
    private final double[] lat;
    private final double[] lon;

    private Grafo(int n, Arista[][] adyacencia, long[] osmid, double[] x, double[] y,
                  double[] lat, double[] lon) {
        this.n = n;
        this.adyacencia = adyacencia;
        this.osmid = osmid;
        this.x = x;
        this.y = y;
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Construye el grafo a partir de arreglos paralelos de tramos u -> v.
     *
     * <p>Si hay tramos paralelos u -> v (el grafo de OSMnx es un MultiDiGraph), se conserva
     * el de menor tiempo, porque es el único que puede estar en un camino de tiempo mínimo.
     * Costo: O(V + E) con un mapa auxiliar por par (u, v).
     *
     * <p>{@code osmid}, {@code x}, {@code y}, {@code lat} y {@code lon} pueden ser
     * {@code null} (pruebas); si no lo son, deben tener tamaño n.
     */
    public static Grafo desdeArreglos(int n, int[] u, int[] v, double[] tiempoS,
                                      double[] distanciaM, long[] osmid, double[] x,
                                      double[] y, double[] lat, double[] lon) {
        if (u.length != v.length || u.length != tiempoS.length || u.length != distanciaM.length) {
            throw new IllegalArgumentException("Los arreglos de aristas deben tener el mismo tamaño");
        }
        Map<Long, Integer> mejor = new HashMap<>();
        List<Integer> elegidas = new ArrayList<>();
        for (int i = 0; i < u.length; i++) {
            if (u[i] < 0 || u[i] >= n || v[i] < 0 || v[i] >= n) {
                throw new IllegalArgumentException(
                        "Arista (" + u[i] + ", " + v[i] + ") fuera de rango 0.." + (n - 1));
            }
            if (tiempoS[i] < 0 || distanciaM[i] < 0) {
                throw new IllegalArgumentException(
                        "Peso negativo en (" + u[i] + ", " + v[i] + "): Dijkstra exige pesos >= 0");
            }
            long clave = (long) u[i] * n + v[i];
            Integer previa = mejor.get(clave);
            if (previa == null) {
                mejor.put(clave, elegidas.size());
                elegidas.add(i);
            } else if (tiempoS[i] < tiempoS[elegidas.get(previa)]) {
                elegidas.set(previa, i);
            }
        }
        List<List<Arista>> listas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            listas.add(new ArrayList<>());
        }
        for (int i : elegidas) {
            listas.get(u[i]).add(new Arista(v[i], tiempoS[i], distanciaM[i]));
        }
        Arista[][] adyacencia = new Arista[n][];
        for (int i = 0; i < n; i++) {
            adyacencia[i] = listas.get(i).toArray(new Arista[0]);
        }
        long[] ids = osmid != null ? osmid.clone() : new long[n];
        if (osmid == null) {
            for (int i = 0; i < n; i++) {
                ids[i] = i;
            }
        }
        return new Grafo(n, adyacencia, ids, copiaOVacio(x, n), copiaOVacio(y, n),
                copiaOVacio(lat, n), copiaOVacio(lon, n));
    }

    /** Atajo para pruebas: filas {u, v, peso}; el peso se usa como tiempo y distancia. */
    public static Grafo dirigido(int n, double[][] aristas) {
        int m = aristas.length;
        int[] u = new int[m];
        int[] v = new int[m];
        double[] t = new double[m];
        double[] d = new double[m];
        for (int i = 0; i < m; i++) {
            u[i] = (int) aristas[i][0];
            v[i] = (int) aristas[i][1];
            t[i] = aristas[i][2];
            d[i] = aristas[i].length > 3 ? aristas[i][3] : aristas[i][2];
        }
        return desdeArreglos(n, u, v, t, d, null, null, null, null, null);
    }

    /** Atajo para pruebas: cada fila {u, v, peso} se agrega en ambos sentidos. */
    public static Grafo noDirigido(int n, double[][] aristas) {
        double[][] dobles = new double[aristas.length * 2][];
        for (int i = 0; i < aristas.length; i++) {
            dobles[2 * i] = new double[] {aristas[i][0], aristas[i][1], aristas[i][2]};
            dobles[2 * i + 1] = new double[] {aristas[i][1], aristas[i][0], aristas[i][2]};
        }
        return dirigido(n, dobles);
    }

    private static double[] copiaOVacio(double[] arreglo, int n) {
        return arreglo != null ? arreglo.clone() : new double[0];
    }

    public int n() {
        return n;
    }

    public int numAristas() {
        int total = 0;
        for (Arista[] lista : adyacencia) {
            total += lista.length;
        }
        return total;
    }

    /** Vecinos de salida de u. No modificar el arreglo devuelto. */
    public Arista[] vecinos(int u) {
        return adyacencia[u];
    }

    public long osmid(int u) {
        return osmid[u];
    }

    public double x(int u) {
        return x[u];
    }

    public double y(int u) {
        return y[u];
    }

    public double lat(int u) {
        return lat[u];
    }

    public double lon(int u) {
        return lon[u];
    }

    /** Copias de las coordenadas UTM (para R1.4 y R3). */
    public double[] coordenadasX() {
        return x.clone();
    }

    public double[] coordenadasY() {
        return y.clone();
    }
}
