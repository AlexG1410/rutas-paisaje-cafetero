package co.edu.uniquindio.rutas;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.IntStream;

/**
 * Constantes del enunciado compartidas por todas las capas (como config.py en Python).
 * Todo número que fija el enunciado vive aquí: no repetirlo en otras clases.
 *
 * <p>Los algoritmos de {@code algorithm} NO usan esta clase: reciben todo por
 * parámetro (son puros).
 */
public final class Constantes {

    private Constantes() {
    }

    // --- Rutas del repositorio (se ejecuta desde la raíz; -Drutas.raiz=... para cambiarla) ---
    public static final Path RAIZ =
            Path.of(System.getProperty("rutas.raiz", ".")).toAbsolutePath().normalize();
    public static final Path DATA = RAIZ.resolve("data");
    public static final Path DATA_PROCESSED = DATA.resolve("processed");
    public static final Path CATALOGO = DATA.resolve("catalogo").resolve("lugares.csv");
    public static final Path RESULTS = RAIZ.resolve("results");
    public static final Path FIGURAS = RESULTS.resolve("figuras");

    // --- Datos (sección 4 y R1) ---
    public static final int EPSG_UTM_18N = 32618;
    public static final double UMBRAL_LUGAR_NODO_M = 500.0;
    public static final String ID_PARTIDA_DEFECTO = "cab_armenia";
    public static final SequencedMap<String, Integer> MINIMOS_CATALOGO;
    public static final int TOTAL_MINIMO_CATALOGO = 40;

    static {
        SequencedMap<String, Integer> minimos = new LinkedHashMap<>();
        minimos.put("cabecera", 12);
        minimos.put("atractivo", 15);
        minimos.put("finca_hotel", 8);
        minimos.put("acopio_trilladora", 5);
        MINIMOS_CATALOGO = Collections.unmodifiableSequencedMap(minimos);
    }

    // --- Protocolo experimental (sección 6) ---
    /** 6.3: cada medición se repite 5 veces y se reporta la mediana. */
    public static final int REPETICIONES = 5;
    /** Ejecuciones de calentamiento de la JVM (JIT) que no se cuentan; ver protocolo. */
    public static final int CALENTAMIENTOS = 1;
    /** 6.3: ejecuciones de más de 1 minuto se miden una sola vez. */
    public static final double UMBRAL_UNA_SOLA_MEDICION_S = 60.0;
    /** 6.7: más de 5 minutos: se detiene y se reporta "no completada". */
    public static final double LIMITE_TIEMPO_S = 300.0;
    /** 6.5: semilla fija para todo dato aleatorio. */
    public static final long SEMILLA = 2026L;

    // --- R2 ---
    public static final int PARES_COLA_PRIORIDAD_MIN = 20;
    public static final int PARES_LINEAL = 10;

    // --- R3 ---
    public static final List<Integer> TAMANOS_CUADRADO = List.of(1_000, 10_000, 50_000);
    public static final long LADO_CUADRADO = 1_000_000L;

    // --- R4 ---
    public static final List<Integer> K_EXACTAS = IntStream.rangeClosed(5, 12).boxed().toList();
    public static final List<Integer> K_CALIDAD = List.of(5, 8, 10, 12);
    public static final int INSTANCIAS_POR_K = 5;
    public static final int JORNADA_MIN = 11 * 60;
    public static final int ATRACTIVOS_MIN = 6;
    public static final int ATRACTIVOS_MAX = 8;
}
