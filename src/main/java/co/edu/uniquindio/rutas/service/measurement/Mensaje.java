package co.edu.uniquindio.rutas.service.measurement;

import java.io.Serializable;

/**
 * Mensaje de la JVM hija al proceso que mide.
 *
 * <ul>
 *   <li>{@code CALENTAMIENTO}: terminó una ejecución de calentamiento (no se cuenta).</li>
 *   <li>{@code REPETICION}: terminó una repetición medida ({@code ms}).</li>
 *   <li>{@code FIN}: no hay más ejecuciones; {@code resultado} es la salida de la última.</li>
 *   <li>{@code ERROR}: la tarea lanzó una excepción ({@code detalle}).</li>
 * </ul>
 */
record Mensaje(Tipo tipo, double ms, Serializable resultado, String detalle)
        implements Serializable {

    enum Tipo { INICIO, CALENTAMIENTO, REPETICION, FIN, ERROR }
}
