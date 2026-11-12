package co.edu.uniquindio.rutas.model;

/**
 * Lugar del catálogo (sección 4).
 *
 * @param categoria          cabecera | atractivo | finca_hotel | acopio_trilladora
 * @param lat                {@code null} mientras el catálogo no tenga la coordenada
 * @param duracionVisitaMin  solo para atractivos (R4.4); {@code null} en los demás
 */
public record Lugar(String id, String nombre, String municipio, String categoria,
                    Double lat, Double lon, Integer duracionVisitaMin) {
}
