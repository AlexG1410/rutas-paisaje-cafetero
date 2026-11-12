package co.edu.uniquindio.rutas.repository;

import co.edu.uniquindio.rutas.Constantes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * results/r2/matriz_tiempos.csv (R2 -> R4, R5): encabezado {@code id, <ids...>} y una fila
 * por lugar de origen; celdas en segundos con 3 decimales.
 */
@Repository
public class MatrizRepository {

    /** Matriz leída: ids en el orden de filas y columnas. */
    public record MatrizLeida(List<String> ids, double[][] matriz) {
    }

    private final Path archivo;

    public MatrizRepository() {
        this(Constantes.RESULTS.resolve("r2").resolve("matriz_tiempos.csv"));
    }

    public MatrizRepository(Path archivo) {
        this.archivo = archivo;
    }

    public boolean existe() {
        return Files.exists(archivo);
    }

    public void guardar(List<String> ids, double[][] matriz) {
        if (matriz.length != ids.size()) {
            throw new IllegalArgumentException("La matriz debe tener una fila por id");
        }
        List<String> encabezado = new ArrayList<>();
        encabezado.add("id");
        encabezado.addAll(ids);
        List<List<Object>> filas = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            if (matriz[i].length != ids.size()) {
                throw new IllegalArgumentException("La matriz debe ser cuadrada");
            }
            List<Object> fila = new ArrayList<>();
            fila.add(ids.get(i));
            for (double valor : matriz[i]) {
                fila.add(String.format(Locale.ROOT, "%.3f", valor));
            }
            filas.add(fila);
        }
        Csv.escribir(archivo, encabezado, filas);
    }

    public MatrizLeida leer() {
        List<Map<String, String>> filas = Csv.leer(archivo);
        List<String> ids = new ArrayList<>(filas.getFirst().keySet());
        ids.removeFirst(); // columna "id"
        double[][] matriz = new double[filas.size()][ids.size()];
        for (int i = 0; i < filas.size(); i++) {
            if (!filas.get(i).get("id").equals(ids.get(i))) {
                throw new IllegalStateException("Los ids de filas y columnas no coinciden");
            }
            for (int j = 0; j < ids.size(); j++) {
                matriz[i][j] = Double.parseDouble(filas.get(i).get(ids.get(j)));
            }
        }
        return new MatrizLeida(List.copyOf(ids), matriz);
    }
}
