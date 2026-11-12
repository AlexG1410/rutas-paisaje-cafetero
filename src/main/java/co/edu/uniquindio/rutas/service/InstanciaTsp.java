package co.edu.uniquindio.rutas.service;

import java.io.Serializable;

/**
 * Entrada de un método del viajante para medirlo en una JVM aparte (R4).
 * Convención de {@code lugares}: ver {@code algorithm.Tsp}.
 */
public record InstanciaTsp(double[][] matriz, int[] lugares) implements Serializable {
}
