package co.edu.uniquindio.rutas.service.measurement;

import java.io.Serializable;

/**
 * Algoritmo que se puede medir en una JVM aparte ({@link Medidor#medirConLimite}).
 *
 * <p>La implementación debe ser una clase pública con constructor público sin argumentos
 * (la JVM hija la crea por reflexión) y la entrada y la salida deben ser {@link Serializable}.
 * Solo debe llamar al algoritmo: nada de leer archivos ni imprimir.
 */
public interface Tarea<E extends Serializable, R extends Serializable> {

    R ejecutar(E entrada);
}
