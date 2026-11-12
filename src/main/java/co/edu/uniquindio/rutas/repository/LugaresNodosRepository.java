package co.edu.uniquindio.rutas.repository;

import co.edu.uniquindio.rutas.Constantes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Repository;

/** data/processed/lugares_nodos.csv: id_lugar, idx_nodo, distancia_m, mas_de_500m, tratamiento. */
@Repository
public class LugaresNodosRepository {

    public static final List<String> COLUMNAS =
            List.of("id_lugar", "idx_nodo", "distancia_m", "mas_de_500m", "tratamiento");

    private final Path archivo;

    public LugaresNodosRepository() {
        this(Constantes.DATA_PROCESSED.resolve("lugares_nodos.csv"));
    }

    public LugaresNodosRepository(Path archivo) {
        this.archivo = archivo;
    }

    public boolean existe() {
        return Files.exists(archivo);
    }

    public void guardar(List<FilaLugarNodo> filas) {
        Csv.escribir(archivo, COLUMNAS, filas.stream()
                .map(f -> List.<Object>of(f.idLugar(), f.idxNodo(), f.distanciaM(), f.masDe500m(),
                        f.tratamiento() == null ? "" : f.tratamiento()))
                .toList());
    }

    public List<FilaLugarNodo> leer() {
        return Csv.leer(archivo).stream()
                .map(f -> new FilaLugarNodo(f.get("id_lugar"), Integer.parseInt(f.get("idx_nodo")),
                        Double.parseDouble(f.get("distancia_m")),
                        Boolean.parseBoolean(f.get("mas_de_500m")), f.get("tratamiento")))
                .toList();
    }
}
