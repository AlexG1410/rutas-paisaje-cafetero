package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.algorithm.EnvolventeConvexa;
import co.edu.uniquindio.rutas.model.Punto;
import co.edu.uniquindio.rutas.service.measurement.Tarea;
import java.io.Serializable;
import java.util.ArrayList;

/** Adaptador de transporte: calcular devuelve una ArrayList serializable sin copia extra. */
public final class TareaEnvolvente implements Tarea<ArrayList<Punto>, Serializable> {
    @Override public Serializable ejecutar(ArrayList<Punto> puntos) {
        return (Serializable) EnvolventeConvexa.calcular(puntos);
    }
}
