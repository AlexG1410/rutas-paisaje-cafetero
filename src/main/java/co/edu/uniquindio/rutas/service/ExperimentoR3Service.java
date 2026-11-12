package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import co.edu.uniquindio.rutas.model.Punto;
import co.edu.uniquindio.rutas.Constantes;
import co.edu.uniquindio.rutas.algorithm.EnvolventeConvexa;
import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.service.measurement.Medicion;
import co.edu.uniquindio.rutas.service.measurement.Medidor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * R3 — Experimentos de la envolvente convexa (spec: specs/R3-envolvente-convexa.md).
 *
 * <p>Pasos de {@link #ejecutar()}:
 * <ol>
 *   <li>Preparar (fuera de la medición) los 4 conjuntos: nodos de la red con
 *       {@link #redondearYDeduplicar} y cuadrados de 1 000, 10 000 y 50 000 puntos con
 *       {@code new java.util.Random(Constantes.SEMILLA)}.</li>
 *   <li>Exportar cada conjunto → results/r3/puntos_&lt;conjunto&gt;.csv (x, y) para que
 *       python/ mida SciPy sobre EXACTAMENTE los mismos puntos.</li>
 *   <li>Medir {@code EnvolventeConvexa.calcular} → results/r3/envolvente_propio.csv
 *       (conjunto, n, vertices, tiempo_propio_ms, repeticiones, semilla) y los vértices de
 *       cada conjunto → results/r3/vertices_&lt;conjunto&gt;.csv.</li>
 *   <li>Vértices de la red con lat/lon → results/r3/envolvente_red.csv (para R5).</li>
 * </ol>
 * Después: {@code python -m preparacion envolvente-scipy} verifica contra SciPy, mide SciPy
 * y escribe la tabla final results/r3/envolvente.csv con el cociente.
 */
@Service
public class ExperimentoR3Service {

    private final RedService redService;
    private final ResultadosRepository resultados;

    public ExperimentoR3Service(RedService redService, ResultadosRepository resultados) {
        this.redService = redService;
        this.resultados = resultados;
    }

    public void ejecutar() {
        Grafo grafo = redService.grafo();
        double[] xs = grafo.coordenadasX();
        double[] ys = grafo.coordenadasY();
        if (xs.length != grafo.n() || ys.length != grafo.n() || grafo.n() == 0) {
            throw new IllegalStateException("La red requiere coordenadas UTM completas");
        }
        Map<String, List<Punto>> conjuntos = new LinkedHashMap<>();
        conjuntos.put("red", redondearYDeduplicar(xs, ys));
        for (int cantidad : Constantes.TAMANOS_CUADRADO) {
            Random azar = new Random(Constantes.SEMILLA);
            Set<Punto> puntos = new LinkedHashSet<>();
            for (int i = 0; i < cantidad; i++) {
                puntos.add(new Punto(azar.nextLong(Constantes.LADO_CUADRADO),
                        azar.nextLong(Constantes.LADO_CUADRADO)));
            }
            conjuntos.put("cuadrado_" + cantidad, List.copyOf(puntos));
        }
        Map<String, List<Punto>> vertices = new LinkedHashMap<>();
        // Verificar TODOS los conjuntos antes de medir el primero.
        for (var conjunto : conjuntos.entrySet()) {
            validarRango(conjunto.getValue());
            var casco = EnvolventeConvexa.calcular(conjunto.getValue());
            verificar(conjunto.getValue(), casco);
            if (casco.size() < 3) {
                throw new IllegalStateException("El experimento exige conjuntos no colineales");
            }
            vertices.put(conjunto.getKey(), casco);
        }
        var mapa = filasMapa(grafo, xs, ys, vertices.get("red"));
        List<Map<String, Object>> tabla = new ArrayList<>();
        for (var conjunto : conjuntos.entrySet()) {
            Medicion m = medir(new ArrayList<>(conjunto.getValue()));
            if (m.completada() && (!vertices.get(conjunto.getKey()).equals(m.resultado())
                    || m.medianaMs() == null || !Double.isFinite(m.medianaMs()) || m.medianaMs() < 0)) {
                throw new IllegalStateException("Medición de envolvente incorrecta: " + conjunto.getKey());
            }
            tabla.add(fila(ResultadosRepository.COLUMNAS_ENVOLVENTE_PROPIO_R3,
                    conjunto.getKey(), conjunto.getValue().size(), vertices.get(conjunto.getKey()).size(),
                    m.medianaMs(), m.repeticiones(), Constantes.SEMILLA, m.estado()));
        }
        // No publicar resultados de una serie con verificaciones fallidas.
        for (String nombre : conjuntos.keySet()) {
            resultados.guardar("r3/puntos_" + nombre + ".csv", filasPuntos(conjuntos.get(nombre)));
            resultados.guardar("r3/vertices_" + nombre + ".csv", filasPuntos(vertices.get(nombre)));
        }
        resultados.guardar("r3/envolvente_red.csv", mapa);
        resultados.guardar("r3/envolvente_propio.csv", tabla);
    }

    Medicion medir(ArrayList<Punto> puntos) {
        return Medidor.medirConLimite(TareaEnvolvente.class, puntos);
    }

    private static void validarRango(List<Punto> puntos) {
        long minX = Collections.min(puntos).x();
        long maxX = Collections.max(puntos).x();
        long minY = puntos.stream().mapToLong(Punto::y).min().orElseThrow();
        long maxY = puntos.stream().mapToLong(Punto::y).max().orElseThrow();
        try {
            if (Math.subtractExact(maxX, minX) <= 1_000_000_000L
                    && Math.subtractExact(maxY, minY) <= 1_000_000_000L) {
                return;
            }
        } catch (ArithmeticException e) {
            throw new IllegalStateException("El rango de coordenadas desborda long", e);
        }
        throw new IllegalStateException("Rango demasiado grande para el experimento con long");
    }

    /** Certificado geométrico O(n·h), fuera del reloj; no vuelve a construir otro casco. */
    private static void verificar(List<Punto> puntos, List<Punto> casco) {
        Set<Punto> entrada = new HashSet<>(puntos);
        if (casco.isEmpty() || !entrada.containsAll(casco)
                || new HashSet<>(casco).size() != casco.size()
                || !casco.getFirst().equals(Collections.min(puntos))) {
            throw new IllegalStateException("Vértices de envolvente inválidos");
        }
        if (casco.size() < 3) {
            if (casco.size() == 1 && entrada.size() == 1) {
                return;
            }
            if (casco.size() == 2 && casco.getLast().equals(Collections.max(puntos))
                    && puntos.stream().allMatch(p -> EnvolventeConvexa.orientacion(
                            casco.getFirst(), casco.getLast(), p) == 0)) {
                return;
            }
            throw new IllegalStateException("Envolvente degenerada inválida");
        }
        for (int i = 0; i < casco.size(); i++) {
            Punto a = casco.get(i);
            Punto b = casco.get((i + 1) % casco.size());
            if (EnvolventeConvexa.orientacion(a, b, casco.get((i + 2) % casco.size())) <= 0) {
                throw new IllegalStateException("Envolvente no estrictamente CCW");
            }
            for (Punto p : puntos) {
                if (EnvolventeConvexa.orientacion(a, b, p) < 0) {
                    throw new IllegalStateException("La envolvente no contiene todos los puntos");
                }
            }
        }
    }

    private static List<Map<String, Object>> filasPuntos(List<Punto> puntos) {
        return puntos.stream().map(p -> fila(ResultadosRepository.COLUMNAS_PUNTOS_R3, p.x(), p.y())).toList();
    }

    private static List<Map<String, Object>> filasMapa(Grafo grafo, double[] xs, double[] ys,
                                                       List<Punto> casco) {
        Map<Punto, Integer> primero = new HashMap<>();
        for (int i = 0; i < xs.length; i++) {
            primero.putIfAbsent(new Punto((long) Math.floor(xs[i] + 0.5),
                    (long) Math.floor(ys[i] + 0.5)), i);
        }
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Punto p : casco) {
            int nodo = primero.get(p);
            try {
                filas.add(fila(ResultadosRepository.COLUMNAS_ENVOLVENTE_RED_R3, filas.size(),
                        p.x(), p.y(), nodo, grafo.lat(nodo), grafo.lon(nodo)));
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalStateException("Faltan coordenadas geográficas para el mapa", e);
            }
        }
        return filas;
    }

    private static Map<String, Object> fila(List<String> columnas, Object... valores) {
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 0; i < columnas.size(); i++) {
            fila.put(columnas.get(i), valores[i]);
        }
        return fila;
    }

    /**
     * Aproxima cada coordenada al entero más cercano ({@code Math.floor(v + 0.5)}, los .5
     * hacia arriba) y elimina los repetidos conservando el orden de primera aparición. NO
     * ordena: si los puntos llegaran ordenados, el algoritmo medido se ahorraría su propio
     * ordenamiento y la comparación con SciPy quedaría sesgada.
     */
    public static List<Punto> redondearYDeduplicar(double[] xs, double[] ys) {
        if (xs == null || ys == null || xs.length != ys.length) {
            throw new IllegalArgumentException("Arreglos de coordenadas nulos o de distinto tamaño");
        }
        Set<Punto> unicos = new LinkedHashSet<>();
        for (int i = 0; i < xs.length; i++) {
            for (double valor : new double[] {xs[i], ys[i]}) {
                double entero = Math.floor(valor + 0.5);
                if (!Double.isFinite(valor) || entero < -0x1.0p63 || entero >= 0x1.0p63) {
                    throw new IllegalArgumentException("Coordenada no finita o fuera de long");
                }
            }
            unicos.add(new Punto((long) Math.floor(xs[i] + 0.5), (long) Math.floor(ys[i] + 0.5)));
        }
        return List.copyOf(unicos);
    }
}
