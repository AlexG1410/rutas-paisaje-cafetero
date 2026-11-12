package co.edu.uniquindio.rutas.repository;

import co.edu.uniquindio.rutas.Constantes;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Lectura del catálogo de lugares (sección 4 del enunciado). */
@Repository
public class CatalogoRepository {

    public static final List<String> COLUMNAS = List.of("id", "nombre", "municipio",
            "categoria", "lat", "lon", "duracion_visita_min", "fuente_duracion",
            "fuente_coordenadas", "verificado_mapa", "notas");

    private final Path archivo;

    public CatalogoRepository() {
        this(Constantes.CATALOGO);
    }

    public CatalogoRepository(Path archivo) {
        this.archivo = archivo;
    }

    /** Columnas presentes en el archivo (para validar el formato). */
    public List<String> columnas() {
        List<Map<String, String>> filas = Csv.leer(archivo);
        return filas.isEmpty() ? List.of() : List.copyOf(filas.getFirst().keySet());
    }

    public List<FilaCatalogo> leer() {
        return Csv.leer(archivo).stream().map(CatalogoRepository::convertir).toList();
    }

    private static FilaCatalogo convertir(Map<String, String> f) {
        return new FilaCatalogo(texto(f, "id"), texto(f, "nombre"), texto(f, "municipio"),
                texto(f, "categoria"), decimal(f, "lat"), decimal(f, "lon"),
                entero(f, "duracion_visita_min"), texto(f, "fuente_duracion"),
                texto(f, "fuente_coordenadas"), texto(f, "verificado_mapa"), texto(f, "notas"));
    }

    private static String texto(Map<String, String> f, String columna) {
        String valor = f.getOrDefault(columna, "").strip();
        return valor.isEmpty() ? null : valor;
    }

    private static Double decimal(Map<String, String> f, String columna) {
        String valor = texto(f, columna);
        return valor == null ? null : Double.valueOf(valor);
    }

    private static Integer entero(Map<String, String> f, String columna) {
        String valor = texto(f, columna);
        return valor == null ? null : (int) Math.round(Double.parseDouble(valor));
    }
}
