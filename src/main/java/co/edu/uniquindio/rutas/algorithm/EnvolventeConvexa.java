package co.edu.uniquindio.rutas.algorithm;

import co.edu.uniquindio.rutas.model.Punto;
import java.util.List;

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
        throw new UnsupportedOperationException("Pendiente R3 — ver specs/R3-envolvente-convexa.md");
    }

    /**
     * Vértices de la envolvente en sentido antihorario, empezando por el menor (x, y), sin
     * repetir el primero al final. Casos degenerados: ver la spec.
     *
     * <p>Algoritmo elegido por el equipo (divide y vencerás): documentar aquí su recurrencia y
     * su complejidad en tiempo y memoria (incluida la pila de recursión), con fuente.
     */
    public static List<Punto> calcular(List<Punto> puntos) {
        throw new UnsupportedOperationException("Pendiente R3 — ver specs/R3-envolvente-convexa.md");
    }
}
