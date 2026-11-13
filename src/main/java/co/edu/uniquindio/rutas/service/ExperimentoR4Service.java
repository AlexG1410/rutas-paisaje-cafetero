package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.MatrizRepository;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import org.springframework.stereotype.Service;
import co.edu.uniquindio.rutas.Constantes;
import co.edu.uniquindio.rutas.algorithm.HeldKarp;
import co.edu.uniquindio.rutas.algorithm.TspFuerzaBruta;
import co.edu.uniquindio.rutas.algorithm.VecinoMasCercano;
import co.edu.uniquindio.rutas.model.ResultadoTsp;
import co.edu.uniquindio.rutas.service.measurement.Medicion;
import co.edu.uniquindio.rutas.service.measurement.Medidor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * R4 — Experimentos del viajante asimétrico (spec: specs/R4-ruta-optima.md).
 *
 * <p>Pasos de {@link #ejecutar()}:
 * <ol>
 *   <li>Instancias: partida Armenia + k lugares al azar (semilla derivada de
 *       {@code Constantes.SEMILLA}) → results/r4/instancias.csv.</li>
 *   <li>VERIFICAR (6.6): fuerza bruta = Held-Karp en cada k que la fuerza bruta resuelva.</li>
 *   <li>Exactas k = 5..12 con {@code Medidor.medirConLimite(TareasTsp.FuerzaBruta.class, ...)}
 *       y {@code TareasTsp.HeldKarpTarea} → results/r4/exactas.csv. Al primer k "no
 *       completada" de un método, no se prueban k mayores para ese método.</li>
 *   <li>Memoria de Held-Karp por k → results/r4/memoria_held_karp.csv.</li>
 *   <li>Calidad de la heurística (k = 5, 8, 10, 12; ≥ 5 instancias) →
 *       results/r4/calidad_heuristica.csv.</li>
 *   <li>Itinerario de un día ({@link ItinerarioService#planear}) → results/r4/itinerario.csv
 *       y results/r4/itinerario_resumen.csv.</li>
 *   <li>Contraejemplo voraz vs óptimo → results/r4/voraz_vs_optimo.csv.</li>
 * </ol>
 */
@Service
public class ExperimentoR4Service {

    private final MatrizRepository matriz;
    private final CatalogoService catalogoService;
    private final ItinerarioService itinerarioService;
    private final ResultadosRepository resultados;
    private final EntornoService entorno;

    public ExperimentoR4Service(MatrizRepository matriz, CatalogoService catalogoService,
                                ItinerarioService itinerarioService,
                                 ResultadosRepository resultados, EntornoService entorno) {
        this.matriz = matriz;
        this.catalogoService = catalogoService;
        this.itinerarioService = itinerarioService;
        this.resultados = resultados;
        this.entorno = entorno;
    }

    public void ejecutar() {
        var leida = matriz.leer();
        ItinerarioService.validarDatos(leida, catalogoService.lugares());
        int partida = leida.ids().indexOf("cab_armenia");
        if (partida < 0 || leida.ids().size() < 13) {
            throw new IllegalStateException("R4 requiere Armenia y al menos doce destinos en matriz R2");
        }
        verificarManuales();
        Map<String, Salida> salidas = new LinkedHashMap<>();
        List<Map<String, Object>> instancias = new ArrayList<>();
        Map<Integer, InstanciaTsp> exactas = new LinkedHashMap<>();
        Map<Integer, ResultadoTsp> referencias = new LinkedHashMap<>();
        for (int k : Constantes.K_EXACTAS) {
            long semilla = Constantes.SEMILLA + k;
            InstanciaTsp instancia = preparar(leida, partida, k, semilla);
            ResultadoTsp referencia = HeldKarp.resolver(instancia.matriz(), instancia.lugares());
            verificar(instancia, referencia, referencia.costoS());
            exactas.put(k, instancia);
            referencias.put(k, referencia);
            instancias.add(fila(ResultadosRepository.COLUMNAS_INSTANCIAS_R4, "exacta", k, 1,
                    semilla, "cab_armenia", recorrido(leida.ids(), instancia.lugares())));
        }
        // Todas las verificaciones rápidas y el itinerario preceden las series de tiempos.
        prepararCalidad(leida, partida, instancias, salidas);
        prepararItinerario(salidas);
        agregar(salidas, "instancias", ResultadosRepository.COLUMNAS_INSTANCIAS_R4, instancias);
        List<Map<String, Object>> tiempos = new ArrayList<>();
        List<Map<String, Object>> memoria = new ArrayList<>();
        boolean seguirBf = true;
        boolean seguirHk = true;
        for (var entrada : exactas.entrySet()) {
            int k = entrada.getKey();
            var instancia = entrada.getValue();
            var referencia = referencias.get(k);
            Serie bf = new Serie(Medicion.noCompletada(List.of()), null, "no_ejecutada");
            if (seguirBf) {
                Medicion previa = medir(instancia, true, true);
                if (previa.completada()) {
                    validarMedicion(instancia, previa, referencia.costoS());
                    bf = serie(instancia, true, referencia.costoS());
                } else {
                    bf = new Serie(previa, null, "verificacion");
                }
                seguirBf = bf.medicion().completada();
            }
            Serie hk = seguirHk ? serie(instancia, false, referencia.costoS())
                    : new Serie(Medicion.noCompletada(List.of()), null, "no_ejecutada");
            seguirHk = hk.medicion().completada();
            tiempos.add(fila(ResultadosRepository.COLUMNAS_EXACTAS_R4, k,
                    bf.medicion().medianaMs(), bf.medicion().estado(), hk.medicion().medianaMs(), hk.medicion().estado(),
                    bf.resultado() == null ? null : bf.resultado().costoS(),
                    hk.resultado() == null ? null : hk.resultado().costoS(),
                    bf.resultado() != null && hk.resultado() != null ? true : null,
                    bf.medicion().repeticiones(), hk.medicion().repeticiones(), Constantes.SEMILLA + k, bf.fase(), hk.fase()));
            long celdas = HeldKarp.entradasTabla(k);
            memoria.add(fila(ResultadosRepository.COLUMNAS_MEMORIA_R4, k, celdas, 8 * celdas, 4 * celdas, 12 * celdas));
        }
        agregar(salidas, "exactas", ResultadosRepository.COLUMNAS_EXACTAS_R4, tiempos);
        agregar(salidas, "memoria_held_karp", ResultadosRepository.COLUMNAS_MEMORIA_R4, memoria);
        entorno.registrarEn("r4/maquina_java.csv");
        for (var salida : salidas.entrySet()) {
            resultados.guardar("r4/" + salida.getKey() + ".csv", salida.getValue().columnas(), salida.getValue().filas());
        }
    }

    private record Serie(Medicion medicion, ResultadoTsp resultado, String fase) { }
    private record Salida(List<String> columnas, List<Map<String, Object>> filas) { }

    Medicion medir(InstanciaTsp instancia, boolean fuerzaBruta, boolean verificacion) {
        if (verificacion) {
            return Medidor.medirConLimite(TareasTsp.FuerzaBruta.class, instancia, 1, 0,
                    Constantes.UMBRAL_UNA_SOLA_MEDICION_S, Constantes.LIMITE_TIEMPO_S);
        }
        return fuerzaBruta ? Medidor.medirConLimite(TareasTsp.FuerzaBruta.class, instancia)
                : Medidor.medirConLimite(TareasTsp.HeldKarpTarea.class, instancia);
    }

    private Serie serie(InstanciaTsp instancia, boolean fuerzaBruta, double esperado) {
        Medicion m = medir(instancia, fuerzaBruta, false);
        return new Serie(m, m.completada() ? validarMedicion(instancia, m, esperado) : null, "medicion");
    }

    private static ResultadoTsp validarMedicion(InstanciaTsp instancia, Medicion m, double esperado) {
        if (!(m.resultado() instanceof ResultadoTsp r) || m.medianaMs() == null
                || !Double.isFinite(m.medianaMs()) || m.medianaMs() < 0) {
            throw new IllegalStateException("Medición TSP sin resultado o tiempo válido");
        }
        verificar(instancia, r, esperado);
        return r;
    }

    /** Comprobación independiente de pertenencia y suma, sin usar Tsp.costoRecorrido. */
    private static void verificar(InstanciaTsp instancia, ResultadoTsp resultado, double esperado) {
        if (resultado == null || resultado.recorrido() == null || !Double.isFinite(resultado.costoS())
                || Math.abs(resultado.costoS() - esperado) > 1e-6
                || resultado.recorrido().length != instancia.lugares().length + 1) {
            throw new IllegalStateException("Costo o tamaño de recorrido TSP incorrecto");
        }
        int[] ruta = resultado.recorrido();
        int p = instancia.lugares()[0];
        if (ruta[0] != p || ruta[ruta.length - 1] != p) throw new IllegalStateException("Falta salida o regreso");
        var pendientes = new HashSet<Integer>();
        for (int i = 1; i < instancia.lugares().length; i++) pendientes.add(instancia.lugares()[i]);
        double costoS = 0;
        for (int i = 1; i < ruta.length; i++) {
            if (i < ruta.length - 1 && !pendientes.remove(ruta[i])) {
                throw new IllegalStateException("Visita omitida, repetida o ajena");
            }
            costoS += instancia.matriz()[ruta[i - 1]][ruta[i]];
        }
        if (!pendientes.isEmpty() || !Double.isFinite(costoS) || Math.abs(costoS - resultado.costoS()) > 1e-6) {
            throw new IllegalStateException("Suma de desplazamientos incorrecta");
        }
    }

    private static void verificarManuales() {
        double[][] guia = {{0, 10, 15, 20}, {10, 0, 35, 25}, {15, 35, 0, 30}, {20, 25, 30, 0}};
        double[][] asimetrica = {{0, 1, 9}, {9, 0, 1}, {1, 9, 0}};
        for (var instancia : List.of(new InstanciaTsp(guia, new int[] {0, 1, 2, 3}),
                new InstanciaTsp(asimetrica, new int[] {0, 1, 2}))) {
            double esperado = instancia.lugares().length == 4 ? 80 : 3;
            verificar(instancia, HeldKarp.resolver(instancia.matriz(), instancia.lugares()), esperado);
            verificar(instancia, TspFuerzaBruta.resolver(instancia.matriz(), instancia.lugares()), esperado);
        }
    }

    private static InstanciaTsp preparar(MatrizRepository.MatrizLeida leida, int partida, int k, long semilla) {
        List<Integer> candidatos = new ArrayList<>();
        for (int i = 0; i < leida.ids().size(); i++) if (i != partida) candidatos.add(i);
        Collections.shuffle(candidatos, new Random(semilla));
        int[] lugares = new int[k + 1];
        lugares[0] = partida;
        for (int i = 0; i < k; i++) lugares[i + 1] = candidatos.get(i);
        return new InstanciaTsp(leida.matriz(), lugares);
    }

    private void prepararCalidad(MatrizRepository.MatrizLeida leida, int partida,
                                 List<Map<String, Object>> instancias, Map<String, Salida> salidas) {
        List<Map<String, Object>> detalle = new ArrayList<>();
        List<Map<String, Object>> resumen = new ArrayList<>();
        List<Map<String, Object>> contraejemplos = new ArrayList<>();
        var manual = new InstanciaTsp(new double[][] {{0, 7, 8, 6}, {7, 0, 9, 2}, {8, 9, 0, 1}, {6, 2, 1, 0}},
                new int[] {0, 1, 2, 3});
        var optimoManual = HeldKarp.resolver(manual.matriz(), manual.lugares());
        var vorazManual = VecinoMasCercano.resolver(manual.matriz(), manual.lugares());
        verificar(manual, optimoManual, 18);
        verificar(manual, vorazManual, 23);
        contraejemplos.add(filaVoraz("T4-b", List.of("A", "B", "C", "D"), optimoManual, vorazManual));
        for (int k : Constantes.K_CALIDAD) {
            double suma = 0;
            double maxima = 0;
            for (int numero = 1; numero <= Constantes.INSTANCIAS_POR_K; numero++) {
                long semilla = Constantes.SEMILLA + 1000L * k + numero;
                var instancia = preparar(leida, partida, k, semilla);
                var hk = HeldKarp.resolver(instancia.matriz(), instancia.lugares());
                var voraz = VecinoMasCercano.resolver(instancia.matriz(), instancia.lugares());
                verificar(instancia, hk, hk.costoS());
                verificar(instancia, voraz, voraz.costoS());
                double brecha = ItinerarioService.brecha(hk.costoS(), voraz.costoS());
                suma += brecha;
                maxima = Math.max(maxima, brecha);
                instancias.add(fila(ResultadosRepository.COLUMNAS_INSTANCIAS_R4, "calidad", k, numero,
                        semilla, "cab_armenia", recorrido(leida.ids(), instancia.lugares())));
                detalle.add(fila(ResultadosRepository.COLUMNAS_CALIDAD_DETALLE_R4, k, numero, semilla,
                        hk.costoS(), voraz.costoS(), brecha, recorrido(leida.ids(), hk.recorrido()), recorrido(leida.ids(), voraz.recorrido())));
                if (brecha > 0 && contraejemplos.size() == 1) {
                    contraejemplos.add(filaVoraz("real_k" + k + "_" + numero, leida.ids(), hk, voraz));
                }
            }
            resumen.add(fila(ResultadosRepository.COLUMNAS_CALIDAD_R4, k, Constantes.INSTANCIAS_POR_K,
                    suma / Constantes.INSTANCIAS_POR_K, maxima));
        }
        agregar(salidas, "calidad_detalle", ResultadosRepository.COLUMNAS_CALIDAD_DETALLE_R4, detalle);
        agregar(salidas, "calidad_heuristica", ResultadosRepository.COLUMNAS_CALIDAD_R4, resumen);
        agregar(salidas, "voraz_vs_optimo", ResultadosRepository.COLUMNAS_VORAZ_R4, contraejemplos);
    }

    private void prepararItinerario(Map<String, Salida> salidas) {
        var iniciales = Constantes.ATRACTIVOS_ITINERARIO;
        var inicial = itinerarioService.comparar("cab_armenia", iniciales).optimo();
        var finalizado = itinerarioService.planear("cab_armenia", iniciales);
        List<Map<String, Object>> paradas = new ArrayList<>();
        for (var p : finalizado.paradas()) {
            paradas.add(fila(ResultadosRepository.COLUMNAS_ITINERARIO_R4, p.orden(), p.idLugar(), p.nombre(),
                    p.desplazamientoMin(), p.visitaMin(), p.tiempoAcumuladoH()));
        }
        var pendientes = new ArrayList<>(iniciales);
        List<Map<String, Object>> retiros = new ArrayList<>();
        double antes = inicial.duracionJornadaMin();
        for (String id : finalizado.retirados()) {
            pendientes.remove(id);
            double despues = itinerarioService.comparar("cab_armenia", pendientes).optimo().duracionJornadaMin();
            retiros.add(fila(ResultadosRepository.COLUMNAS_RETIROS_R4, retiros.size() + 1, id, antes, despues, antes - despues));
            antes = despues;
        }
        agregar(salidas, "itinerario", ResultadosRepository.COLUMNAS_ITINERARIO_R4, paradas);
        agregar(salidas, "retiros", ResultadosRepository.COLUMNAS_RETIROS_R4, retiros);
        agregar(salidas, "itinerario_resumen", ResultadosRepository.COLUMNAS_RESUMEN_R4,
                List.of(fila(ResultadosRepository.COLUMNAS_RESUMEN_R4, "cab_armenia", String.join(";", iniciales),
                        String.join(";", pendientes), String.join(";", finalizado.retirados()), finalizado.totalVisitasMin(),
                        finalizado.totalDesplazamientosMin(), finalizado.duracionJornadaMin(), Constantes.JORNADA_MIN,
                        inicial.duracionJornadaMin())));
    }

    private static Map<String, Object> filaVoraz(String caso, List<String> ids, ResultadoTsp hk, ResultadoTsp voraz) {
        return fila(ResultadosRepository.COLUMNAS_VORAZ_R4, caso, hk.recorrido().length - 2,
                hk.costoS(), voraz.costoS(), ItinerarioService.brecha(hk.costoS(), voraz.costoS()),
                recorrido(ids, hk.recorrido()), recorrido(ids, voraz.recorrido()));
    }

    private static String recorrido(List<String> ids, int[] recorrido) {
        List<String> nombres = new ArrayList<>();
        for (int indice : recorrido) nombres.add(ids.get(indice));
        return String.join(";", nombres);
    }

    private static void agregar(Map<String, Salida> salidas, String nombre, List<String> columnas,
                                List<Map<String, Object>> filas) {
        salidas.put(nombre, new Salida(columnas, filas));
    }

    private static Map<String, Object> fila(List<String> columnas, Object... valores) {
        if (columnas.size() != valores.length) throw new IllegalArgumentException("Columnas y valores de R4 no coinciden");
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 0; i < columnas.size(); i++) fila.put(columnas.get(i), valores[i]);
        return fila;
    }
}
