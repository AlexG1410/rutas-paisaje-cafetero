package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.LugaresNodosRepository;
import co.edu.uniquindio.rutas.repository.MatrizRepository;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import co.edu.uniquindio.rutas.Constantes;
import co.edu.uniquindio.rutas.algorithm.Dijkstra;
import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.model.Lugar;
import co.edu.uniquindio.rutas.model.ResultadoDijkstra;
import co.edu.uniquindio.rutas.service.measurement.Medicion;
import co.edu.uniquindio.rutas.service.measurement.Medidor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * R2 — Experimentos de Dijkstra y matriz de tiempos (spec: specs/R2-caminos-minimos.md).
 *
 * <p>Pasos de {@link #ejecutar()}:
 * <ol>
 *   <li>Elegir ≥ 20 pares (10 cercanos para la versión lineal) → results/r2/pares.csv.</li>
 *   <li>VERIFICAR (6.6): en los 10 pares, las dos versiones dan el mismo tiempo (1e-6 s)
 *       → results/r2/verificacion.csv; abortar si no.</li>
 *   <li>Medir con {@code Medidor.medirConLimite} → results/r2/mediciones_dijkstra.csv
 *       (origen, destino, version, tiempo_viaje_s, nodos_explorados, tiempo_ms,
 *       repeticiones, estado).</li>
 *   <li>Cociente lineal / cola en los 10 pares → results/r2/cociente.csv (mediana, minimo,
 *       maximo).</li>
 *   <li>Matriz de tiempos con {@code Dijkstra.colaPrioridad(..., SIN_DESTINO)} desde cada
 *       lugar → results/r2/matriz_tiempos.csv ({@link MatrizRepository}).</li>
 * </ol>
 * Después: {@code python -m preparacion verificar-dijkstra} compara los pares con NetworkX.
 */
@Service
public class ExperimentoR2Service {

    private final RedService redService;
    private final CatalogoService catalogoService;
    private final LugaresNodosRepository lugaresNodos;
    private final MatrizRepository matriz;
    private final ResultadosRepository resultados;

    public ExperimentoR2Service(RedService redService, CatalogoService catalogoService,
                                LugaresNodosRepository lugaresNodos, MatrizRepository matriz,
                                ResultadosRepository resultados) {
        this.redService = redService;
        this.catalogoService = catalogoService;
        this.lugaresNodos = lugaresNodos;
        this.matriz = matriz;
        this.resultados = resultados;
    }

    public void ejecutar() {
        var errores = catalogoService.validarArchivo();
        if (!errores.isEmpty()) {
            throw new IllegalStateException("Catálogo inválido: " + errores);
        }
        Grafo grafo = redService.grafo();
        List<Lugar> lugares = new ArrayList<>(catalogoService.lugares());
        lugares.sort(Comparator.comparing(Lugar::id));
        Map<String, Integer> nodos = asociaciones(lugares, grafo.n());
        List<Par> pares = elegirPares(lugares, nodos);
        double[][] tiemposMatriz = construirMatriz(grafo, lugares, nodos);
        Map<String, Integer> indiceLugar = new HashMap<>();
        for (int i = 0; i < lugares.size(); i++) {
            indiceLugar.put(lugares.get(i).id(), i);
        }

        // Toda verificación previa termina antes de la primera medición o publicación.
        List<Map<String, Object>> verificacion = new ArrayList<>();
        List<ResultadoDijkstra> colas = new ArrayList<>();
        List<ResultadoDijkstra> lineales = new ArrayList<>();
        for (Par par : pares) {
            double esperado = tiemposMatriz[indiceLugar.get(par.origen().id())]
                    [indiceLugar.get(par.destino().id())];
            var cola = Dijkstra.colaPrioridad(grafo, par.nodoOrigen(), par.nodoDestino());
            validarResultado(grafo, par, cola, esperado);
            colas.add(cola);
            ResultadoDijkstra lineal = null;
            if (par.lineal()) {
                lineal = Dijkstra.lineal(grafo, par.nodoOrigen(), par.nodoDestino());
                validarResultado(grafo, par, lineal, cola.tiempoS());
                verificacion.add(fila(ResultadosRepository.COLUMNAS_VERIFICACION_R2,
                        par.origen().id(), par.destino().id(), lineal.tiempoS(), cola.tiempoS(), true));
            }
            lineales.add(lineal);
        }

        List<Map<String, Object>> mediciones = new ArrayList<>();
        List<Map<String, Object>> cocientes = new ArrayList<>();
        List<Double> valoresCociente = new ArrayList<>();
        for (int i = 0; i < pares.size(); i++) {
            Par par = pares.get(i);
            var entrada = new InstanciaDijkstra(grafo, par.nodoOrigen(), par.nodoDestino());
            Medicion cola = medir(entrada, false);
            mediciones.add(filaMedicion(grafo, par, cola, colas.get(i), false));
            if (par.lineal()) {
                Medicion lineal = medir(entrada, true);
                mediciones.add(filaMedicion(grafo, par, lineal, lineales.get(i), true));
                Double cociente = null;
                if (cola.completada() && lineal.completada() && cola.medianaMs() > 0) {
                    cociente = lineal.medianaMs() / cola.medianaMs();
                    if (!Double.isFinite(cociente)) {
                        throw new IllegalStateException("Cociente no finito");
                    }
                    valoresCociente.add(cociente);
                }
                cocientes.add(fila(ResultadosRepository.COLUMNAS_COCIENTE_R2,
                        par.origen().id(), par.destino().id(), cociente,
                        cociente == null ? "no disponible" : "completada"));
            }
        }
        agregarResumen(cocientes, valoresCociente);
        List<Map<String, Object>> filasPares = new ArrayList<>();
        for (Par par : pares) {
            filasPares.add(fila(ResultadosRepository.COLUMNAS_PARES_R2,
                    par.origen().id(), par.destino().id(), par.nodoOrigen(), par.nodoDestino(),
                    par.origen().municipio(), par.destino().municipio(),
                    par.lineal() ? "mismo_municipio" : "intermunicipal", par.lineal(), Constantes.SEMILLA));
        }
        resultados.guardar("r2/pares.csv", filasPares);
        resultados.guardar("r2/verificacion.csv", verificacion);
        resultados.guardar("r2/mediciones_dijkstra.csv", mediciones);
        resultados.guardar("r2/cociente.csv", cocientes);
        matriz.guardar(lugares.stream().map(Lugar::id).toList(), tiemposMatriz);
    }

    /** Punto de sustitución en pruebas: tareas reales se miden exclusivamente en JVM hija. */
    Medicion medir(InstanciaDijkstra entrada, boolean lineal) {
        return lineal ? Medidor.medirConLimite(TareasDijkstra.Lineal.class, entrada)
                : Medidor.medirConLimite(TareasDijkstra.Cola.class, entrada);
    }

    private Map<String, Integer> asociaciones(List<Lugar> lugares, int n) {
        Map<String, Integer> nodos = new HashMap<>();
        for (var fila : lugaresNodos.leer()) {
            if (fila.idxNodo() < 0 || fila.idxNodo() >= n
                    || nodos.putIfAbsent(fila.idLugar(), fila.idxNodo()) != null) {
                throw new IllegalStateException("Asociación duplicada o nodo inválido: " + fila.idLugar());
            }
        }
        Set<String> ids = new HashSet<>();
        for (Lugar lugar : lugares) {
            ids.add(lugar.id());
        }
        if (!ids.equals(nodos.keySet()) || !ids.contains(Constantes.ID_PARTIDA_DEFECTO)) {
            throw new IllegalStateException("Asociaciones y catálogo no coinciden o falta Armenia");
        }
        return nodos;
    }

    private record Par(Lugar origen, Lugar destino, int nodoOrigen, int nodoDestino, boolean lineal) {
    }

    private static List<Par> elegirPares(List<Lugar> lugares, Map<String, Integer> nodos) {
        List<Par> cercanos = new ArrayList<>();
        List<Par> otros = new ArrayList<>();
        for (Lugar origen : lugares) {
            for (Lugar destino : lugares) {
                int u = nodos.get(origen.id());
                int v = nodos.get(destino.id());
                if (origen.id().equals(destino.id()) || u == v) {
                    continue;
                }
                boolean cercano = origen.municipio().equals(destino.municipio());
                (cercano ? cercanos : otros).add(new Par(origen, destino, u, v, cercano));
            }
        }
        Random azar = new Random(Constantes.SEMILLA);
        Collections.shuffle(cercanos, azar);
        Collections.shuffle(otros, azar);
        int intermunicipales = Constantes.PARES_COLA_PRIORIDAD_MIN - Constantes.PARES_LINEAL;
        if (cercanos.size() < Constantes.PARES_LINEAL || otros.size() < intermunicipales) {
            throw new IllegalStateException("No hay suficientes pares distintos para el criterio aprobado");
        }
        List<Par> pares = new ArrayList<>(cercanos.subList(0, Constantes.PARES_LINEAL));
        pares.addAll(otros.subList(0, intermunicipales));
        return pares;
    }

    private static double[][] construirMatriz(Grafo grafo, List<Lugar> lugares, Map<String, Integer> nodos) {
        double[][] matriz = new double[lugares.size()][lugares.size()];
        for (int i = 0; i < lugares.size(); i++) {
            double[] dist = Dijkstra.colaPrioridad(grafo, nodos.get(lugares.get(i).id()),
                    Dijkstra.SIN_DESTINO).distancias();
            for (int j = 0; j < lugares.size(); j++) {
                double valor = dist[nodos.get(lugares.get(j).id())];
                if (!Double.isFinite(valor) || valor < 0 || (i == j && valor != 0)) {
                    throw new IllegalStateException("Matriz no finita o diagonal inválida; revisar SCC");
                }
                matriz[i][j] = valor;
            }
        }
        return matriz;
    }

    private static void validarResultado(Grafo grafo, Par par, ResultadoDijkstra r, double esperado) {
        if (r == null || !Double.isFinite(r.tiempoS()) || Math.abs(r.tiempoS() - esperado) > 1e-6
                || r.ruta() == null || r.ruta().length == 0 || r.ruta()[0] != par.nodoOrigen()
                || r.ruta()[r.ruta().length - 1] != par.nodoDestino()
                || r.nodosExplorados() < r.ruta().length || r.nodosExplorados() > grafo.n()) {
            throw new IllegalStateException("Resultado Dijkstra incorrecto para " + par.origen().id());
        }
        boolean[] visitado = new boolean[grafo.n()];
        double costoS = 0;
        for (int i = 0; i < r.ruta().length; i++) {
            int u = r.ruta()[i];
            if (u < 0 || u >= grafo.n() || visitado[u]) {
                throw new IllegalStateException("Ruta con nodo inválido o repetido");
            }
            visitado[u] = true;
            if (i + 1 < r.ruta().length) {
                int v = r.ruta()[i + 1];
                double pesoS = Double.POSITIVE_INFINITY;
                for (var arista : grafo.vecinos(u)) {
                    if (arista.destino() == v) {
                        pesoS = arista.tiempoS();
                        break;
                    }
                }
                costoS += pesoS;
            }
        }
        if (!Double.isFinite(costoS) || Math.abs(costoS - r.tiempoS()) > 1e-6) {
            throw new IllegalStateException("Costo o aristas de la ruta incorrectos");
        }
    }

    private static Map<String, Object> filaMedicion(Grafo grafo, Par par, Medicion m,
                                                    ResultadoDijkstra esperado, boolean lineal) {
        ResultadoDijkstra r = null;
        String ruta = "";
        if (m.completada()) {
            if (!(m.resultado() instanceof ResultadoDijkstra obtenido) || m.medianaMs() == null
                    || !Double.isFinite(m.medianaMs()) || m.medianaMs() < 0) {
                throw new IllegalStateException("Medición completada sin resultado o tiempo válido");
            }
            r = obtenido;
            validarResultado(grafo, par, r, esperado.tiempoS());
            if (r.nodosExplorados() != esperado.nodosExplorados()) {
                throw new IllegalStateException("Cambió el número de nodos explorados entre ejecuciones");
            }
            ruta = String.join("|", Arrays.stream(r.ruta()).mapToObj(String::valueOf).toList());
        }
        return fila(ResultadosRepository.COLUMNAS_MEDICIONES_R2, par.origen().id(), par.destino().id(),
                lineal ? "lineal" : "cola_prioridad", r == null ? null : r.tiempoS(),
                r == null ? null : r.nodosExplorados(), m.medianaMs(), m.repeticiones(), m.estado(), ruta);
    }

    private static void agregarResumen(List<Map<String, Object>> filas, List<Double> valores) {
        valores.sort(Double::compare);
        boolean completo = valores.size() == Constantes.PARES_LINEAL;
        int mitad = valores.size() / 2;
        Double mediana = null;
        if (completo) {
            mediana = valores.size() % 2 == 0
                    ? (valores.get(mitad - 1) + valores.get(mitad)) / 2 : valores.get(mitad);
        }
        Object[] resumen = {mediana, completo ? valores.getFirst() : null,
                completo ? valores.getLast() : null};
        String[] nombres = {"mediana", "minimo", "maximo"};
        for (int i = 0; i < nombres.length; i++) {
            filas.add(fila(ResultadosRepository.COLUMNAS_COCIENTE_R2, nombres[i], "", resumen[i],
                    completo ? "completada" : "no disponible"));
        }
    }

    private static Map<String, Object> fila(List<String> columnas, Object... valores) {
        if (columnas.size() != valores.length) {
            throw new IllegalArgumentException("Cantidad de columnas y valores diferente");
        }
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 0; i < columnas.size(); i++) {
            fila.put(columnas.get(i), valores[i]);
        }
        return fila;
    }
}
