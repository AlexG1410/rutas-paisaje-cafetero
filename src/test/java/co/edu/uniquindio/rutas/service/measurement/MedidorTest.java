package co.edu.uniquindio.rutas.service.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Protocolo experimental (sección 6) aplicado por el arnés. */
class MedidorTest {

    public static final class IdentificarProceso implements Tarea<Integer, Long> {
        @Override public Long ejecutar(Integer ignorado) {
            return ProcessHandle.current().pid();
        }
    }

    @Test void p6jb_recogeHijosTrasExitoYTimeout() {
        var antes = ProcessHandle.current().children().map(ProcessHandle::pid).sorted().toList();
        var m = Medidor.medirConLimite(IdentificarProceso.class, 0, 1, 0, 60, 30);
        assertTrue(m.completada());
        assertTrue((Long) m.resultado() != ProcessHandle.current().pid());
        var lenta = Medidor.medirConLimite(Dormir.class, 2000, 1, 0, 60, 0.1);
        assertFalse(lenta.completada());
        assertEquals(antes, ProcessHandle.current().children().map(ProcessHandle::pid).sorted().toList());
    }

    public static final class PrepararLento implements Tarea<Integer, Integer> {
        public PrepararLento() {
            new Dormir().ejecutar(350);
        }

        @Override public Integer ejecutar(Integer valor) {
            return valor;
        }
    }

    @Test void p6ja_preparacionNoConsumeLimiteExperimental() {
        var m = Medidor.medirConLimite(PrepararLento.class, 123, 1, 0, 60, 0.1);
        assertTrue(m.completada());
        assertEquals(123, m.resultado());
        assertTrue(m.medianaMs() < 100);
    }

    /** Tarea de prueba con calentamiento breve y una repetición posterior lenta. */
    public static final class DemoraPosterior implements Tarea<Integer, Integer> {
        private int llamadas;

        @Override
        public Integer ejecutar(Integer llamadaLenta) {
            llamadas++;
            if (llamadas == llamadaLenta) {
                new Dormir().ejecutar(350);
            }
            return llamadas;
        }
    }

    @Test
    void p63ab_repeticionLargaTrasCalentarConservaSoloEsaMuestra() {
        for (int llamadaLenta : List.of(2, 3)) {
            AtomicInteger llamadas = new AtomicInteger();
            Medicion m = Medidor.medir(() -> {
                int actual = llamadas.incrementAndGet();
                if (actual == llamadaLenta) {
                    new Dormir().ejecutar(350);
                }
                return actual;
            }, 5, 1, 0.2);
            assertEquals(llamadaLenta, llamadas.get());
            assertEquals(llamadaLenta, m.resultado());
            assertEquals(1, m.repeticiones());
            assertTrue(m.medianaMs() >= 200);
        }
    }

    @Test
    void p63d_jvmAisladaDescartaMuestrasBrevesSiUnaPosteriorEsLarga() {
        for (int llamadaLenta : List.of(2, 3)) {
            Medicion m = Medidor.medirConLimite(DemoraPosterior.class, llamadaLenta,
                    5, 1, 0.2, 30);
            assertTrue(m.completada());
            assertEquals(llamadaLenta, m.resultado());
            assertEquals(1, m.repeticiones());
            assertTrue(m.medianaMs() >= 200);
        }
    }

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
