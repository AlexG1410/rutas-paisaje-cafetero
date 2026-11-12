package co.edu.uniquindio.rutas.repository;

import co.edu.uniquindio.rutas.Constantes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.JacksonException;

/**
 * Lee la red procesada que exporta python/ (R1.5). CONTRATO de archivos (spec R1, "Formato"):
 *
 * <pre>
 * data/processed/nodos.csv.gz    idx, osmid, lat, lon, x_utm, y_utm      (idx = 0..V-1)
 * data/processed/aristas.csv.gz  u, v, longitud_m, velocidad_kmh, fuente_velocidad,
 *                                tiempo_s, highway                        (u, v en idx)
 * data/processed/lugares_utm.csv id_lugar, x_utm, y_utm                   (R1.4)
 * </pre>
 * Cambiar una columna es cambiar el contrato: actualizar antes la spec y python/.
 */
@Repository
public class RedRepository {

    public static final List<String> COLUMNAS_NODOS =
            List.of("idx", "osmid", "lat", "lon", "x_utm", "y_utm");
    public static final List<String> COLUMNAS_ARISTAS = List.of("u", "v", "longitud_m",
            "velocidad_kmh", "fuente_velocidad", "tiempo_s", "highway");

    private final Path directorio;

    public RedRepository() {
        this(Constantes.DATA_PROCESSED);
    }

    public RedRepository(Path directorio) {
        this.directorio = directorio;
    }

    public boolean existe() {
        return Files.exists(directorio.resolve("nodos.csv.gz"))
                && Files.exists(directorio.resolve("aristas.csv.gz"));
    }

    /** R1.1: lee solo metadatos exportados por Python, nunca el GraphML. */
    public ConteosRed leerConteosRed() {
        try {
            JsonNode datos = JsonMapper.builder().build().readTree(
                    Files.readString(directorio.resolve("metadatos.json")));
            return new ConteosRed(enteroPositivo(datos, "nodos_descargada"),
                    enteroPositivo(datos, "aristas_descargada"),
                    enteroPositivo(datos, "nodos_componente"),
                    enteroPositivo(datos, "aristas_componente"));
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("No se pudo leer metadatos.json de la red", e);
        }
    }

    private static int enteroPositivo(JsonNode datos, String campo) {
        JsonNode valor = datos == null ? null : datos.get(campo);
        if (valor == null || !valor.isIntegralNumber() || !valor.canConvertToInt()
                || valor.intValue() <= 0) {
            throw new IllegalStateException("metadatos.json: entero positivo inválido en " + campo);
        }
        return valor.intValue();
    }

    public RedLeida leer() {
        List<Map<String, String>> nodos = Csv.leer(directorio.resolve("nodos.csv.gz"));
        List<Map<String, String>> aristas = Csv.leer(directorio.resolve("aristas.csv.gz"));
        exigirColumnas(nodos, COLUMNAS_NODOS, "nodos.csv.gz");
        exigirColumnas(aristas, COLUMNAS_ARISTAS, "aristas.csv.gz");

        int n = nodos.size();
        long[] osmid = new long[n];
        double[] lat = new double[n];
        double[] lon = new double[n];
        double[] x = new double[n];
        double[] y = new double[n];
        boolean[] visto = new boolean[n];
        for (Map<String, String> fila : nodos) {
            int idx = Integer.parseInt(fila.get("idx"));
            if (idx < 0 || idx >= n || visto[idx]) {
                throw new IllegalStateException("nodos.idx debe ser 0..V-1 sin huecos ni repetidos");
            }
            visto[idx] = true;
            osmid[idx] = Long.parseLong(fila.get("osmid"));
            lat[idx] = Double.parseDouble(fila.get("lat"));
            lon[idx] = Double.parseDouble(fila.get("lon"));
            x[idx] = Double.parseDouble(fila.get("x_utm"));
            y[idx] = Double.parseDouble(fila.get("y_utm"));
        }

        int m = aristas.size();
        int[] u = new int[m];
        int[] v = new int[m];
        double[] tiempo = new double[m];
        double[] longitud = new double[m];
        for (int i = 0; i < m; i++) {
            Map<String, String> fila = aristas.get(i);
            u[i] = Integer.parseInt(fila.get("u"));
            v[i] = Integer.parseInt(fila.get("v"));
            tiempo[i] = Double.parseDouble(fila.get("tiempo_s"));
            longitud[i] = Double.parseDouble(fila.get("longitud_m"));
        }
        return new RedLeida(n, u, v, tiempo, longitud, osmid, lat, lon, x, y);
    }

    /**
     * data/processed/lugares_utm.csv (id_lugar, x_utm, y_utm): coordenadas UTM 18N de los
     * lugares del catálogo, calculadas por python/ con pyproj al procesar la red.
     */
    public Map<String, double[]> leerLugaresUtm() {
        List<Map<String, String>> filas = Csv.leer(directorio.resolve("lugares_utm.csv"));
        exigirColumnas(filas, List.of("id_lugar", "x_utm", "y_utm"), "lugares_utm.csv");
        Map<String, double[]> utm = new java.util.LinkedHashMap<>();
        for (Map<String, String> f : filas) {
            if (f.get("id_lugar").isBlank() || utm.containsKey(f.get("id_lugar"))) {
                throw new IllegalStateException("lugares_utm: ID vacío o repetido");
            }
            utm.put(f.get("id_lugar"), new double[] {
                Double.parseDouble(f.get("x_utm")), Double.parseDouble(f.get("y_utm"))});
        }
        return utm;
    }

    static void exigirColumnas(List<Map<String, String>> filas, List<String> columnas,
                               String archivo) {
        if (filas.isEmpty()) {
            throw new IllegalStateException(archivo + " está vacío");
        }
        List<String> faltan = columnas.stream()
                .filter(c -> !filas.getFirst().containsKey(c)).toList();
        if (!faltan.isEmpty()) {
            throw new IllegalStateException(archivo + ": faltan columnas " + faltan);
        }
    }
}
