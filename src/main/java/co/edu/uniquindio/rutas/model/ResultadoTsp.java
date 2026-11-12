package co.edu.uniquindio.rutas.model;

import java.io.Serializable;

/**
 * Resultado de un método del viajante (R4).
 *
 * @param costoS     tiempo total del recorrido en segundos
 * @param recorrido  empieza y termina en la partida; cada lugar a visitar aparece una vez
 *                   (longitud k + 2)
 */
public record ResultadoTsp(double costoS, int[] recorrido) implements Serializable {
}
