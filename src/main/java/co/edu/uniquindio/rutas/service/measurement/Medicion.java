package co.edu.uniquindio.rutas.service.measurement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado de medir un algoritmo según el protocolo (sección 6).
 *
 * @param tiemposMs  tiempos de las repeticiones MEDIDAS, en milisegundos
 * @param completada false si alguna ejecución superó el límite de 5 min (6.7)
 * @param resultado  salida de la última ejecución, para verificarla (puede ser null)
 */
public record Medicion(List<Double> tiemposMs, boolean completada, Object resultado) {

    public Medicion {
        tiemposMs = List.copyOf(tiemposMs);
    }

    public static Medicion noCompletada(List<Double> tiemposParciales) {
        return new Medicion(tiemposParciales, false, null);
    }

    /** Mediana en ms, o null si no se completó o no hay tiempos. */
    public Double medianaMs() {
        if (!completada || tiemposMs.isEmpty()) {
            return null;
        }
        List<Double> ordenados = new ArrayList<>(tiemposMs);
        ordenados.sort(null);
        int m = ordenados.size();
        return m % 2 == 1 ? ordenados.get(m / 2)
                : (ordenados.get(m / 2 - 1) + ordenados.get(m / 2)) / 2.0;
    }

    public int repeticiones() {
        return tiemposMs.size();
    }

    public String estado() {
        return completada ? "completada" : "no completada";
    }

    /** Columnas estándar para los CSV de resultados: tiempo_ms, repeticiones, estado. */
    public Map<String, Object> comoFila() {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("tiempo_ms", medianaMs());
        fila.put("repeticiones", repeticiones());
        fila.put("estado", estado());
        return fila;
    }
}
