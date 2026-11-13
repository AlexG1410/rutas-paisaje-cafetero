package co.edu.uniquindio.rutas.repository;

import co.edu.uniquindio.rutas.Constantes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Repository;

/**
 * Archivos de results/ (protocolo 6.8). Todos los resultados se guardan en CSV; las figuras
 * (PNG) las genera python/ a partir de estos CSV. Nombres relativos a results/, por ejemplo
 * {@code "r2/mediciones_dijkstra.csv"}.
 */
@Repository
public class ResultadosRepository {

    public static final List<String> COLUMNAS_LUGARES_LEJANOS = LugaresNodosRepository.COLUMNAS;
    public static final List<String> COLUMNAS_MEMORIA_REPRESENTACION =
            List.of("red", "representacion", "supuesto_bytes", "V", "E", "megabytes");
    public static final List<String> COLUMNAS_PARES_R2 = List.of("origen", "destino",
            "nodo_origen", "nodo_destino", "municipio_origen", "municipio_destino",
            "criterio", "medir_lineal", "semilla");
    public static final List<String> COLUMNAS_VERIFICACION_R2 = List.of("origen", "destino",
            "tiempo_lineal_s", "tiempo_cola_s", "coinciden");
    public static final List<String> COLUMNAS_MEDICIONES_R2 = List.of("origen", "destino", "version",
            "tiempo_viaje_s", "nodos_explorados", "tiempo_ms", "repeticiones", "estado", "ruta_nodos");
    public static final List<String> COLUMNAS_COCIENTE_R2 = List.of("origen", "destino", "cociente", "estado");
    public static final List<String> COLUMNAS_PUNTOS_R3 = List.of("x", "y");
    public static final List<String> COLUMNAS_ENVOLVENTE_PROPIO_R3 = List.of("conjunto", "n", "vertices",
            "tiempo_propio_ms", "repeticiones", "semilla", "estado");
    public static final List<String> COLUMNAS_ENVOLVENTE_RED_R3 =
            List.of("orden", "x", "y", "idx_nodo", "lat", "lon");
    public static final List<String> COLUMNAS_INSTANCIAS_R4 = List.of("tipo", "k", "numero", "semilla", "partida", "lugares");
    public static final List<String> COLUMNAS_EXACTAS_R4 = List.of("k", "tiempo_fuerza_bruta_ms", "estado_fuerza_bruta",
            "tiempo_held_karp_ms", "estado_held_karp", "costo_fuerza_bruta_s", "costo_held_karp_s", "coinciden",
            "repeticiones_fuerza_bruta", "repeticiones_held_karp", "semilla", "fase_fuerza_bruta", "fase_held_karp");
    public static final List<String> COLUMNAS_MEMORIA_R4 = List.of("k", "entradas", "bytes_dp", "bytes_predecesores", "bytes_estimados");
    public static final List<String> COLUMNAS_CALIDAD_DETALLE_R4 = List.of("k", "numero", "semilla", "costo_held_karp_s",
            "costo_heuristica_s", "brecha_pct", "recorrido_held_karp", "recorrido_heuristica");
    public static final List<String> COLUMNAS_CALIDAD_R4 = List.of("k", "instancias", "brecha_promedio_pct", "brecha_maxima_pct");
    public static final List<String> COLUMNAS_ITINERARIO_R4 = List.of("orden", "id_lugar", "lugar", "desplazamiento_min", "visita_min", "tiempo_acumulado_h");
    public static final List<String> COLUMNAS_RESUMEN_R4 = List.of("partida", "atractivos_iniciales", "atractivos_finales", "retirados",
            "total_visitas_min", "total_desplazamientos_min", "duracion_jornada_min", "limite_min", "duracion_inicial_min");
    public static final List<String> COLUMNAS_RETIROS_R4 = List.of("paso", "id_retirado", "duracion_antes_min", "duracion_despues_min", "reduccion_min");
    public static final List<String> COLUMNAS_VORAZ_R4 = List.of("caso", "k", "costo_optimo_s", "costo_voraz_s", "brecha_pct", "recorrido_optimo", "recorrido_voraz");

    /** R1.4: admite cero lejanos y conserva la cabecera. */
    public Path guardarLugaresLejanos(List<FilaLugarNodo> filas) {
        Path ruta = directorio.resolve("r1/lugares_lejanos.csv");
        Csv.escribir(ruta, COLUMNAS_LUGARES_LEJANOS, filas.stream()
                .map(f -> List.<Object>of(f.idLugar(), f.idxNodo(), f.distanciaM(),
                        f.masDe500m(), f.tratamiento() == null ? "" : f.tratamiento()))
                .toList());
        return ruta;
    }

    private final Path directorio;

    public ResultadosRepository() {
        this(Constantes.RESULTS);
    }

    public ResultadosRepository(Path directorio) {
        this.directorio = directorio;
    }

    /** Guarda filas con el orden de columnas de la primera fila. */
    public Path guardar(String nombre, List<? extends Map<String, ?>> filas) {
        if (filas.isEmpty()) {
            throw new IllegalArgumentException("No hay filas para " + nombre);
        }
        return guardar(nombre, List.copyOf(filas.getFirst().keySet()), filas);
    }

    /** Cabecera explícita, incluso sin filas (por ejemplo, un itinerario sin retiros). */
    public Path guardar(String nombre, List<String> encabezado, List<? extends Map<String, ?>> filas) {
        List<List<Object>> valores = new ArrayList<>();
        for (Map<String, ?> fila : filas) {
            List<Object> celdas = new ArrayList<>();
            for (String columna : encabezado) {
                celdas.add(fila.get(columna));
            }
            valores.add(celdas);
        }
        Path ruta = directorio.resolve(nombre);
        Csv.escribir(ruta, encabezado, valores);
        return ruta;
    }

    public List<Map<String, String>> leer(String nombre) {
        return Csv.leer(directorio.resolve(nombre));
    }

    public boolean existe(String nombre) {
        return Files.exists(directorio.resolve(nombre));
    }

    /** Nombres de las figuras PNG de results/figuras. */
    public List<String> figuras() {
        Path carpeta = directorio.resolve("figuras");
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".png")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Bytes de una figura; solo nombres simples (sin rutas) para no salir de la carpeta. */
    public byte[] figura(String nombre) {
        if (!nombre.matches("[A-Za-z0-9_\\-]+\\.png")) {
            throw new IllegalArgumentException("Nombre de figura no válido: " + nombre);
        }
        try {
            return Files.readAllBytes(directorio.resolve("figuras").resolve(nombre));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
