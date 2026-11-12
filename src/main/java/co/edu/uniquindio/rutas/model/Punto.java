package co.edu.uniquindio.rutas.model;

/**
 * Punto del plano con coordenadas ENTERAS de 64 bits (R3: UTM 18N redondeado). Los productos
 * cruz del dominio experimental caben en long; no se garantiza para cualquier pareja
 * de valores long. EnvolventeConvexa detecta desbordamientos con operaciones exactas.
 *
 * <p>Orden natural: por x y, en empate, por y (el "menor (x, y)" de la spec).
 */
public record Punto(long x, long y) implements Comparable<Punto>, java.io.Serializable {

    @Override
    public int compareTo(Punto otro) {
        int porX = Long.compare(x, otro.x);
        return porX != 0 ? porX : Long.compare(y, otro.y);
    }
}
