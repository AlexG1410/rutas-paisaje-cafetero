package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.Constantes;
import co.edu.uniquindio.rutas.repository.CatalogoRepository;
import co.edu.uniquindio.rutas.repository.FilaCatalogo;
import co.edu.uniquindio.rutas.model.Lugar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Reglas del catálogo de lugares (sección 4 del enunciado). */
@Service
public class CatalogoService {

    // Caja amplia alrededor del Quindío para detectar coordenadas invertidas o mal copiadas.
    // No reemplaza la verificación en un mapa (columna verificado_mapa).
    static final double LAT_MIN = 3.9;
    static final double LAT_MAX = 4.9;
    static final double LON_MIN = -76.1;
    static final double LON_MAX = -75.2;

    private final CatalogoRepository repositorio;

    public CatalogoService(CatalogoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Valida data/catalogo/lugares.csv. */
    public List<String> validarArchivo() {
        List<String> faltan = CatalogoRepository.COLUMNAS.stream()
                .filter(c -> !repositorio.columnas().contains(c)).toList();
        if (!faltan.isEmpty()) {
            return List.of("Faltan columnas: " + faltan);
        }
        return validar(repositorio.leer());
    }

    /** Lugares del catálogo (para R2, R4 y la aplicación). */
    public List<Lugar> lugares() {
        return repositorio.leer().stream()
                .map(f -> new Lugar(f.id(), f.nombre(), f.municipio(), f.categoria(), f.lat(),
                        f.lon(), f.duracionVisitaMin()))
                .toList();
    }

    /** Devuelve los problemas encontrados (lista vacía si el catálogo es válido). */
    public static List<String> validar(List<FilaCatalogo> filas) {
        List<String> errores = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        boolean repetidos = false;
        for (FilaCatalogo f : filas) {
            if (f.id() == null || !ids.add(f.id())) {
                repetidos = true;
            }
        }
        if (repetidos) {
            errores.add("Hay ids vacíos o repetidos");
        }

        Set<String> invalidas = filas.stream().map(FilaCatalogo::categoria)
                .filter(c -> c != null && !Constantes.MINIMOS_CATALOGO.containsKey(c))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!invalidas.isEmpty()) {
            errores.add("Categorías no válidas: " + invalidas + " (válidas: "
                    + Constantes.MINIMOS_CATALOGO.keySet() + ")");
        }

        Map<String, Long> conteo = filas.stream().filter(f -> f.categoria() != null)
                .collect(Collectors.groupingBy(FilaCatalogo::categoria, Collectors.counting()));
        Constantes.MINIMOS_CATALOGO.forEach((categoria, minimo) -> {
            long n = conteo.getOrDefault(categoria, 0L);
            if (n < minimo) {
                errores.add("Categoría '" + categoria + "': " + n + " lugares, mínimo " + minimo);
            }
        });
        if (filas.size() < Constantes.TOTAL_MINIMO_CATALOGO) {
            errores.add("El catálogo tiene " + filas.size() + " lugares, mínimo "
                    + Constantes.TOTAL_MINIMO_CATALOGO);
        }

        agregar(errores, "Sin coordenadas", filas, f -> f.lat() == null || f.lon() == null);
        agregar(errores, "Coordenadas fuera del Quindío (¿lat/lon invertidas?)", filas,
                f -> f.lat() != null && f.lon() != null
                        && (f.lat() < LAT_MIN || f.lat() > LAT_MAX
                        || f.lon() < LON_MIN || f.lon() > LON_MAX));
        agregar(errores, "Atractivos sin duración de visita", filas,
                f -> "atractivo".equals(f.categoria())
                        && (f.duracionVisitaMin() == null || f.duracionVisitaMin() <= 0));
        agregar(errores, "Atractivos sin fuente/criterio de duración", filas,
                f -> "atractivo".equals(f.categoria()) && f.fuenteDuracion() == null);
        agregar(errores, "Coordenadas no verificadas en mapa", filas,
                f -> !"si".equalsIgnoreCase(f.verificadoMapa()));
        return errores;
    }

    private static void agregar(List<String> errores, String mensaje, List<FilaCatalogo> filas,
                                Predicate<FilaCatalogo> problema) {
        List<String> ids = filas.stream().filter(problema).map(FilaCatalogo::id).toList();
        if (!ids.isEmpty()) {
            errores.add(mensaje + ": " + ids);
        }
    }
}
