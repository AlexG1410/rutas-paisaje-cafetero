package co.edu.uniquindio.rutas.service;

import co.edu.uniquindio.rutas.repository.CatalogoRepository;
import co.edu.uniquindio.rutas.repository.LugaresNodosRepository;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import co.edu.uniquindio.rutas.repository.FilaLugarNodo;
import co.edu.uniquindio.rutas.Constantes;
import co.edu.uniquindio.rutas.algorithm.NodoMasCercano;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

/**
 * R1 — parte Java del modelo de red (spec: specs/R1-modelo-red.md). La descarga, los pesos,
 * la componente fuertemente conexa y la medición de R1.3 se hacen en python/.
 *
 * <p>Pasos de {@link #ejecutar()}:
 * <ol>
 *   <li>R1.1: estimación compacta de matriz y listas para los tamaños de tres versiones
 *       de red. No mide memoria real de objetos Java ni tiempos.</li>
 *   <li>R1.4: para cada lugar, {@code NodoMasCercano.buscar} (propio) sobre las coordenadas
 *       UTM del grafo → data/processed/lugares_nodos.csv y results/r1/lugares_lejanos.csv
 *       (más de 500 m). Las coordenadas UTM de los lugares las calcula python/ al procesar.</li>
 * </ol>
 * Después: {@code python -m preparacion verificar-lugares} compara contra OSMnx.
 */
@Service
public class ExperimentoR1Service {

    private final RedService redService;
    private final CatalogoRepository catalogo;
    private final LugaresNodosRepository lugaresNodos;
    private final ResultadosRepository resultados;

    public ExperimentoR1Service(RedService redService, CatalogoRepository catalogo,
                                LugaresNodosRepository lugaresNodos,
                                ResultadosRepository resultados) {
        this.redService = redService;
        this.catalogo = catalogo;
        this.lugaresNodos = lugaresNodos;
        this.resultados = resultados;
    }

    public void ejecutar() {
        var errores = new CatalogoService(catalogo).validarArchivo();
        if (!errores.isEmpty()) {
            throw new IllegalStateException("Catálogo inválido: " + errores);
        }
        var filas = catalogo.leer();
        var utm = redService.lugaresUtm();
        var ids = new HashSet<String>();
        for (var fila : filas) {
            ids.add(fila.id());
        }
        if (!ids.equals(utm.keySet())) {
            throw new IllegalStateException("Los IDs de lugares_utm no coinciden con el catálogo");
        }
        var grafo = redService.grafo();
        var conteos = redService.conteosRed();
        List<Map<String, Object>> memoria = new ArrayList<>();
        agregarEstimaciones(memoria, "descargada", conteos.nodosDescargada(),
                conteos.aristasDescargada());
        agregarEstimaciones(memoria, "componente_exportada", conteos.nodosComponente(),
                conteos.aristasComponente());
        agregarEstimaciones(memoria, "grafo_java", grafo.n(), grafo.numAristas());
        double[] xs = grafo.coordenadasX();
        double[] ys = grafo.coordenadasY();
        if (grafo.n() == 0 || xs.length != grafo.n() || ys.length != grafo.n()) {
            throw new IllegalStateException("Faltan coordenadas UTM de la red");
        }
        for (int i = 0; i < xs.length; i++) {
            if (!Double.isFinite(xs[i]) || !Double.isFinite(ys[i])) {
                throw new IllegalStateException("Coordenada de nodo no finita: " + i);
            }
        }
        List<FilaLugarNodo> asociaciones = new ArrayList<>();
        for (var fila : filas) {
            double[] punto = utm.get(fila.id());
            if (punto == null || punto.length != 2
                    || !Double.isFinite(punto[0]) || !Double.isFinite(punto[1])) {
                throw new IllegalStateException("Coordenada UTM inválida: " + fila.id());
            }
            var cercano = NodoMasCercano.buscar(xs, ys, punto[0], punto[1]);
            boolean lejano = cercano.distanciaM() > Constantes.UMBRAL_LUGAR_NODO_M;
            String tratamiento = fila.notas() == null ? "" : fila.notas();
            if (lejano && tratamiento.isBlank()) {
                tratamiento = "Pendiente: revisar acceso y justificar distancia al nodo";
            }
            asociaciones.add(new FilaLugarNodo(fila.id(), cercano.indice(),
                    cercano.distanciaM(), lejano, tratamiento));
        }
        // Ningún archivo se escribe hasta haber validado y calculado todos los lugares.
        lugaresNodos.guardar(asociaciones);
        resultados.guardarLugaresLejanos(asociaciones.stream()
                .filter(FilaLugarNodo::masDe500m).toList());
        resultados.guardar("r1/memoria_representacion.csv", memoria);
    }

    /** Modelo compacto de R1.1: dos filas por red, costo y memoria O(1) por versión. */
    private static void agregarEstimaciones(List<Map<String, Object>> filas,
                                            String red, int v, int e) {
        // Convertir a double ANTES del producto cuadrático evita desbordamiento de int.
        double bytesMatriz = 16.0 * v * v;
        double bytesListas = 8.0 * v + 20.0 * e;
        for (String representacion : List.of("matriz", "listas")) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("red", red);
            fila.put("representacion", representacion);
            fila.put("supuesto_bytes", representacion.equals("matriz")
                    ? "16 por celda (2 pesos de 8); modelo compacto sin objetos JVM"
                    : "8 por cabecera + 20 por entrada (destino 4 + 2 pesos de 8); sin objetos JVM");
            fila.put("V", v);
            fila.put("E", e);
            fila.put("megabytes", (representacion.equals("matriz") ? bytesMatriz : bytesListas)
                    / 1_000_000);
            if (!List.copyOf(fila.keySet()).equals(ResultadosRepository.COLUMNAS_MEMORIA_REPRESENTACION)) {
                throw new IllegalStateException("Cabecera de estimación de memoria incompatible");
            }
            filas.add(fila);
        }
    }
}
