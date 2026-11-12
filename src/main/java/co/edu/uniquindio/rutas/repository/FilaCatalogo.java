package co.edu.uniquindio.rutas.repository;

/**
 * Fila de data/catalogo/lugares.csv tal como está en el archivo (campos vacíos = null).
 * Las reglas de validación están en {@code service.CatalogoService}.
 */
public record FilaCatalogo(String id, String nombre, String municipio, String categoria,
                           Double lat, Double lon, Integer duracionVisitaMin,
                           String fuenteDuracion, String fuenteCoordenadas,
                           String verificadoMapa, String notas) {
}
