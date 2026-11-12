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
        List<String> encabezado = List.copyOf(filas.getFirst().keySet());
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
