package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.algorithm.HeldKarp;
import co.edu.uniquindio.rutas.algorithm.TspFuerzaBruta;
import co.edu.uniquindio.rutas.algorithm.VecinoMasCercano;
import co.edu.uniquindio.rutas.model.ResultadoTsp;
import co.edu.uniquindio.rutas.service.measurement.Tarea;

/**
 * Métodos de R4 envueltos como {@link Tarea} para medirlos con
 * {@code Medidor.medirConLimite} (límite de 5 min en una JVM aparte). Solo llaman al algoritmo.
 */
public final class TareasTsp {

    private TareasTsp() {
    }

    public static final class FuerzaBruta implements Tarea<InstanciaTsp, ResultadoTsp> {
        @Override
        public ResultadoTsp ejecutar(InstanciaTsp e) {
            return TspFuerzaBruta.resolver(e.matriz(), e.lugares());
        }
    }

    public static final class HeldKarpTarea implements Tarea<InstanciaTsp, ResultadoTsp> {
        @Override
        public ResultadoTsp ejecutar(InstanciaTsp e) {
            return HeldKarp.resolver(e.matriz(), e.lugares());
        }
    }

    public static final class Heuristica implements Tarea<InstanciaTsp, ResultadoTsp> {
        @Override
        public ResultadoTsp ejecutar(InstanciaTsp e) {
            return VecinoMasCercano.resolver(e.matriz(), e.lugares());
        }
    }
}
