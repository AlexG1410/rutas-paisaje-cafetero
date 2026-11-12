package co.edu.uniquindio.rutas.service.measurement;

import co.edu.uniquindio.rutas.Constantes;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Arnés de medición que implementa el protocolo experimental (specs/protocolo-experimental.md).
 *
 * <ul>
 *   <li>6.2: solo se cronometra la llamada al algoritmo. Cargar datos, armar el grafo o
 *       imprimir queda FUERA del {@code Supplier}/{@code Tarea}.</li>
 *   <li>Calentamiento (JVM): primero se hacen {@code calentamientos} ejecuciones que no se
 *       cuentan, para que el compilador JIT optimice el código antes de medir. Si una de ellas
 *       ya supera el umbral de 1 min, se toma como la única medición (6.3).</li>
 *   <li>6.3: luego {@code repeticiones} ejecuciones medidas; se reporta la mediana. Si
 *       cualquiera supera 1 min, se detiene y se conserva solo esa ejecución.</li>
 *   <li>6.4: milisegundos con {@link System#nanoTime()}.</li>
 *   <li>6.7: {@link #medirConLimite} corre en una JVM aparte y la termina si una ejecución
 *       pasa del límite: la medición queda "no completada".</li>
 * </ul>
 * Antes de cada ejecución se llama a {@link System#gc()} (sugerencia a la JVM) para que la
 * basura de la ejecución anterior no se recoja dentro de la siguiente.
 */
public final class Medidor {

    private Medidor() {
    }

    /** Mide en este mismo proceso con los valores del protocolo. */
    public static <T> Medicion medir(Supplier<T> algoritmo) {
        return medir(algoritmo, Constantes.REPETICIONES, Constantes.CALENTAMIENTOS,
                Constantes.UMBRAL_UNA_SOLA_MEDICION_S);
    }

    /**
     * Mide {@code algoritmo} en este mismo proceso, sin límite de tiempo. Para algoritmos que
     * con seguridad terminan (Dijkstra, envolvente, Held-Karp con k <= 12).
     */
    public static <T> Medicion medir(Supplier<T> algoritmo, int repeticiones, int calentamientos,
                                     double umbralUnaS) {
        double umbralMs = umbralUnaS * 1000.0;
        Object resultado = null;
        for (int i = 0; i < calentamientos; i++) {
            Ejecucion<T> e = ejecutar(algoritmo);
            if (e.ms() > umbralMs) {
                return new Medicion(List.of(e.ms()), true, e.resultado());
            }
            resultado = e.resultado();
        }
        List<Double> tiempos = new ArrayList<>();
        for (int i = 0; i < repeticiones; i++) {
            Ejecucion<T> e = ejecutar(algoritmo);
            if (e.ms() > umbralMs) {
                return new Medicion(List.of(e.ms()), true, e.resultado());
            }
            tiempos.add(e.ms());
            resultado = e.resultado();
        }
        return new Medicion(tiempos, true, resultado);
    }

    /** Mide en una JVM aparte con los valores del protocolo (límite de 5 min). */
    public static <E extends Serializable, R extends Serializable> Medicion medirConLimite(
            Class<? extends Tarea<E, R>> tarea, E entrada) {
        return medirConLimite(tarea, entrada, Constantes.REPETICIONES, Constantes.CALENTAMIENTOS,
                Constantes.UMBRAL_UNA_SOLA_MEDICION_S, Constantes.LIMITE_TIEMPO_S);
    }

    /**
     * Como {@link #medir} pero en una JVM hija con límite de tiempo POR EJECUCIÓN. Si una
     * ejecución (incluido el calentamiento) no termina en {@code limiteS} segundos, se
     * termina la JVM hija y la medición queda no completada. Arrancar la JVM hija no entra en
     * la medición: los tiempos los toma la propia hija alrededor de cada ejecución.
     */
    public static <E extends Serializable, R extends Serializable> Medicion medirConLimite(
            Class<? extends Tarea<E, R>> tarea, E entrada, int repeticiones, int calentamientos,
            double umbralUnaS, double limiteS) {
        Process proceso = lanzarHija(tarea, repeticiones, calentamientos, umbralUnaS);
        BlockingQueue<Object> buzon = new LinkedBlockingQueue<>();
        Thread lector = new Thread(() -> leerMensajes(proceso, buzon), "lector-medicion");
        lector.setDaemon(true);
        lector.start();
        List<Double> tiempos = new ArrayList<>();
        try (ObjectOutputStream salida = new ObjectOutputStream(proceso.getOutputStream())) {
            salida.writeObject(entrada);
        } catch (IOException e) {
            proceso.destroyForcibly();
            throw new UncheckedIOException("No se pudo enviar la entrada a la JVM hija", e);
        }
        try {
            while (true) {
                Object recibido = buzon.poll((long) (limiteS * 1000), TimeUnit.MILLISECONDS);
                if (recibido == null) {
                    return Medicion.noCompletada(tiempos);
                }
                if (recibido instanceof Exception error) {
                    throw new IllegalStateException("La JVM hija terminó sin responder", error);
                }
                Mensaje m = (Mensaje) recibido;
                switch (m.tipo()) {
                    case CALENTAMIENTO -> { /* no se cuenta */ }
                    case REPETICION -> {
                        if (m.ms() > umbralUnaS * 1000.0) {
                            // La hija se detiene tras esta ejecución; no mezclar muestras breves.
                            tiempos.clear();
                        }
                        tiempos.add(m.ms());
                    }
                    case FIN -> {
                        return new Medicion(tiempos, true, m.resultado());
                    }
                    case ERROR -> throw new IllegalStateException(
                            "La tarea falló en la JVM hija: " + m.detalle());
                    default -> throw new IllegalStateException("Mensaje desconocido");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Medicion.noCompletada(tiempos);
        } finally {
            if (proceso.isAlive()) {
                proceso.destroyForcibly();
            }
        }
    }

    private static Process lanzarHija(Class<?> tarea, int repeticiones, int calentamientos,
                                      double umbralUnaS) {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        long xmxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        List<String> comando = List.of(java, "-Xmx" + xmxMb + "m", "-Xss16m",
                "-cp", classpath(tarea), ProcesoAislado.class.getName(), tarea.getName(),
                String.valueOf(repeticiones), String.valueOf(calentamientos),
                String.valueOf(umbralUnaS));
        try {
            return new ProcessBuilder(comando)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo lanzar la JVM hija", e);
        }
    }

    /** Classpath actual + ubicación de las clases de la tarea y del arnés. */
    private static String classpath(Class<?> tarea) {
        Set<String> entradas = new LinkedHashSet<>();
        for (Class<?> c : List.of(ProcesoAislado.class, tarea)) {
            try {
                entradas.add(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI())
                        .toString());
            } catch (URISyntaxException | NullPointerException e) {
                // Sin ubicación conocida: se usa solo java.class.path.
            }
        }
        for (String entrada : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!entrada.isBlank()) {
                entradas.add(entrada);
            }
        }
        return String.join(File.pathSeparator, entradas);
    }

    private static void leerMensajes(Process proceso, BlockingQueue<Object> buzon) {
        try (ObjectInputStream entrada =
                     new ObjectInputStream(new BufferedInputStream(proceso.getInputStream()))) {
            while (true) {
                Mensaje m = (Mensaje) entrada.readObject();
                buzon.add(m);
                if (m.tipo() == Mensaje.Tipo.FIN || m.tipo() == Mensaje.Tipo.ERROR) {
                    return;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // La hija se cayó (o el padre la terminó por tiempo; en ese caso ya nadie lee).
            buzon.add(e);
        }
    }

    static <T> Ejecucion<T> ejecutar(Supplier<T> algoritmo) {
        System.gc();
        long inicio = System.nanoTime();
        T resultado = algoritmo.get();
        long fin = System.nanoTime();
        return new Ejecucion<>((fin - inicio) / 1e6, resultado);
    }

    record Ejecucion<T>(double ms, T resultado) {
    }
}
