package co.edu.uniquindio.rutas.repository;

/** Fila de data/processed/lugares_nodos.csv (R1.4). */
public record FilaLugarNodo(String idLugar, int idxNodo, double distanciaM, boolean masDe500m,
                            String tratamiento) {
}
