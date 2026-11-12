package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * Consultas de resultados ya generados (results/) para la capa de presentación. La
 * presentación nunca abre archivos: pide los datos a este servicio.
 */
@Service
public class ConsultaResultadosService {

    private static final Map<String, String> GENERADO_POR = Map.of(
            "r1/", "python -m preparacion r1  y  ./gradlew bootRun --args='r1'",
            "r2/", "./gradlew bootRun --args='r2'",
            "r3/", "./gradlew bootRun --args='r3'  y  python -m preparacion envolvente-scipy",
            "r4/", "./gradlew bootRun --args='r4'",
            "figuras/", "python -m preparacion graficas");

    private final ResultadosRepository resultados;

    public ConsultaResultadosService(ResultadosRepository resultados) {
        this.resultados = resultados;
    }

    /** Tabla de results/ (p. ej. "r2/cociente.csv"). */
    public List<Map<String, String>> tabla(String nombre) {
        if (!nombre.matches("r[1-4]/[A-Za-z0-9_\\-]+\\.csv")) {
            throw new IllegalArgumentException("Nombre de resultado no válido: " + nombre);
        }
        if (!resultados.existe(nombre)) {
            throw new NoSuchElementException(
                    "Falta results/" + nombre + ". Se genera con: " + comandoQueGenera(nombre));
        }
        return resultados.leer(nombre);
    }

    public List<String> figuras() {
        return resultados.figuras();
    }

    public byte[] figura(String nombre) {
        return resultados.figura(nombre);
    }

    public static String comandoQueGenera(String nombre) {
        return GENERADO_POR.entrySet().stream().filter(e -> nombre.startsWith(e.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse("ver README");
    }
}
