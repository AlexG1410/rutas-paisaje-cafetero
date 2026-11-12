package co.edu.uniquindio.rutas.service.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Protocolo experimental (sección 6) aplicado por el arnés. */
class MedidorTest {

    /** Tarea de prueba: suma 0..n-1. */
    public static final class Sumar implements Tarea<Integer, Long> {
        @Override
        public Long ejecutar(Integer n) {
            long total = 0;
            for (int i = 0; i < n; i++) {
                total += i;
            }
            return total;
        }
    }

    /** Tarea de prueba: duerme los milisegundos indicados. */
    public static final class Dormir implements Tarea<Integer, String> {
        @Override
        public String ejecutar(Integer ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        }
    }

    @Test
    void medirHaceCincoRepeticionesMedidasYDevuelveLaMediana() {
        Medicion m = Medidor.medir(() -> new Sumar().ejecutar(10_000));
        assertTrue(m.completada());
        assertEquals(5, m.repeticiones(), "el calentamiento no se cuenta");
        assertEquals(49_995_000L, m.resultado());
        double min = m.tiemposMs().stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        double max = m.tiemposMs().stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        assertTrue(min <= m.medianaMs() && m.medianaMs() <= max);
    }

    @Test
    void medianaConNumeroParDeTiempos() {
        Medicion m = new Medicion(List.of(4.0, 1.0, 3.0, 2.0), true, null);
        assertEquals(2.5, m.medianaMs(), 1e-12);
    }

    @Test
    void unaSolaMedicionSiElCalentamientoPasaElUmbral() {
        Medicion m = Medidor.medir(() -> new Dormir().ejecutar(50), 5, 1, 0.01);
        assertEquals(1, m.repeticiones());
        assertTrue(m.tiemposMs().getFirst() >= 50);
    }

    @Test
    void sinCalentamientoTambienRespetaElUmbral() {
        Medicion m = Medidor.medir(() -> new Dormir().ejecutar(30), 5, 0, 0.01);
        assertEquals(1, m.repeticiones());
    }

    @Test
    void medirConLimiteCompletaEnUnaJvmAparte() {
        Medicion m = Medidor.medirConLimite(Sumar.class, 1000, 3, 1, 60, 60);
        assertTrue(m.completada());
        assertEquals(3, m.repeticiones());
        assertEquals(499_500L, m.resultado());
        assertEquals("completada", m.estado());
    }

    @Test
    void medirConLimiteMarcaNoCompletada() {
        Medicion m = Medidor.medirConLimite(Dormir.class, 5_000, 1, 0, 60, 1.0);
        assertFalse(m.completada());
        assertNull(m.medianaMs());
        assertEquals("no completada", m.estado());
        assertNull(m.comoFila().get("tiempo_ms"));
    }
}
