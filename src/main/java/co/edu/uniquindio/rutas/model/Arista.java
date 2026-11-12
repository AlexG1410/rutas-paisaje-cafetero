package co.edu.uniquindio.rutas.model;

/** Tramo dirigido hacia {@code destino} con sus dos pesos (R1.2). */
public record Arista(int destino, double tiempoS, double distanciaM) implements java.io.Serializable {
}
