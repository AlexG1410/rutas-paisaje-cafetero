package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.Punto;
import java.util.List;
import java.util.ArrayList;

/**
 * R3 — Envolvente convexa por divide y vencerás (spec: specs/R3-envolvente-convexa.md).
 * IMPLEMENTACIÓN PROPIA. Entrada: puntos enteros sin repetidos. Los colineales sobre un lado
 * NO son vértices.
 */
public final class EnvolventeConvexa {

    private EnvolventeConvexa() {
    }

    /**
     * Producto cruz (q - p) x (r - p) en aritmética entera exacta (long).
     * Mayor que 0: r está a la izquierda de p->q (giro antihorario); menor que 0: a la
     * derecha; 0: colineales. Tiempo O(1).
     */
    public static long orientacion(Punto p, Punto q, Punto r) {
        if (p == null || q == null || r == null) {
            throw new IllegalArgumentException("Los puntos no pueden ser null");
        }
        try {
            long qx = Math.subtractExact(q.x(), p.x());
            long qy = Math.subtractExact(q.y(), p.y());
            long rx = Math.subtractExact(r.x(), p.x());
            long ry = Math.subtractExact(r.y(), p.y());
            return Math.subtractExact(Math.multiplyExact(qx, ry), Math.multiplyExact(qy, rx));
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El producto cruz desborda long", e);
        }
    }

    /**
     * Vértices de la envolvente en sentido antihorario, empezando por el menor (x, y), sin
     * repetir el primero al final. Casos degenerados: ver la spec.
     *
     * <p>QuickHull: selecciona el más alejado de cada segmento y divide los candidatos
     * exteriores entre sus dos nuevos lados. Rangos disjuntos en una copia, sin ordenar.
     * T(n)=T(n1)+T(n2)+Θ(n), n1+n2<=n-1: mejor Θ(n), particiones equilibradas O(n log n),
     * peor O(n²) por expansión; no hay garantía promedio sin hipótesis de distribución.
     * Memoria O(n+h+d)=O(n), incluida salida h y pila de profundidad d<=n.
     * Fuente de la idea: Barber, Dobkin y Huhdanpaa (1996), referencia del README de Qhull;
     * derivación propia de esta variante en docs/documento-tecnico/04-r3-envolvente.md.
     * Devuelve ArrayList serializable para transporte local, sin modificar la entrada.
     */
    public static List<Punto> calcular(List<Punto> puntos) {
        if (puntos == null) {
            throw new IllegalArgumentException("La lista no puede ser null");
        }
        Punto[] copia = puntos.toArray(new Punto[0]);
        ArrayList<Punto> salida = new ArrayList<>();
        if (copia.length == 0) {
            return salida;
        }
        Punto minimo = null;
        Punto maximo = null;
        for (Punto punto : copia) {
            if (punto == null) {
                throw new IllegalArgumentException("La lista contiene un punto null");
            }
            if (minimo == null || punto.compareTo(minimo) < 0) {
                minimo = punto;
            }
            if (maximo == null || punto.compareTo(maximo) > 0) {
                maximo = punto;
            }
        }
        salida.add(minimo);
        if (minimo.equals(maximo)) {
            return salida;
        }
        int inferior = particionar(copia, 0, copia.length, minimo, maximo);
        int superior = particionar(copia, inferior, copia.length, maximo, minimo);
        construir(copia, 0, inferior, minimo, maximo, salida);
        construir(copia, inferior, superior, maximo, minimo, salida);
        salida.removeLast(); // La segunda cadena termina en el mínimo ya agregado.
        return salida;
    }

    /** Al entrar, todos los candidatos están estrictamente a la derecha de a->b. */
    private static void construir(Punto[] puntos, int inicio, int fin, Punto a, Punto b,
                                   ArrayList<Punto> salida) {
        if (inicio == fin) {
            salida.add(b);
            return;
        }
        Punto pivote = puntos[inicio];
        long menorCruz = orientacion(a, b, pivote);
        for (int i = inicio + 1; i < fin; i++) {
            long cruz = orientacion(a, b, puntos[i]);
            if (cruz < menorCruz || (cruz == menorCruz && puntos[i].compareTo(pivote) < 0)) {
                pivote = puntos[i];
                menorCruz = cruz;
            }
        }
        // El triángulo a-pivote-b no puede contener nuevos vértices estrictos.
        int primero = particionar(puntos, inicio, fin, a, pivote);
        int segundo = particionar(puntos, primero, fin, pivote, b);
        construir(puntos, inicio, primero, a, pivote, salida);
        construir(puntos, primero, segundo, pivote, b, salida);
    }

    /** Mueve al inicio del rango los puntos a la derecha; devuelve el fin exclusivo. */
    private static int particionar(Punto[] puntos, int inicio, int fin, Punto a, Punto b) {
        int limite = inicio;
        for (int i = inicio; i < fin; i++) {
            if (orientacion(a, b, puntos[i]) < 0) {
                Punto temporal = puntos[limite];
                puntos[limite++] = puntos[i];
                puntos[i] = temporal;
            }
        }
        return limite;
    }
}
