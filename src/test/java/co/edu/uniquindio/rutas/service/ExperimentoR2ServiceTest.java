package co.edu.uniquindio.rutas.service;

import static org.junit.jupiter.api.Assertions.*;

import co.edu.uniquindio.rutas.algorithm.Dijkstra;
import co.edu.uniquindio.rutas.model.ResultadoDijkstra;
import co.edu.uniquindio.rutas.model.Grafo;
import co.edu.uniquindio.rutas.repository.*;
import co.edu.uniquindio.rutas.service.measurement.Medicion;
import co.edu.uniquindio.rutas.service.measurement.Medidor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integración con CSV temporales y medidor sustituido, nunca mediciones del proyecto. */
class ExperimentoR2ServiceTest {
    @TempDir Path carpeta;

    @Test void t2ia_transporteSerializableEnAmbasTareas() {
        var entrada = new InstanciaDijkstra(Grafo.dirigido(2,
                new double[][] {{0, 1, 2.5}}), 0, 1);
        var lineal = Medidor.medirConLimite(TareasDijkstra.Lineal.class, entrada, 1, 0, 60, 30);
        var cola = Medidor.medirConLimite(TareasDijkstra.Cola.class, entrada, 1, 0, 60, 30);
        for (var medicion : List.of(lineal, cola)) {
            assertTrue(medicion.completada());
            var r = (ResultadoDijkstra) medicion.resultado();
            assertEquals(2.5, r.tiempoS());
            assertArrayEquals(new int[] {0, 1}, r.ruta());
            assertEquals(2, r.nodosExplorados());
        }
    }

    private class ExperimentoPrueba extends ExperimentoR2Service {
        int llamadas;
        boolean timeout;
        boolean incorrecto;

        ExperimentoPrueba() {
            super(new RedService(new RedRepository(carpeta)),
                    new CatalogoService(new CatalogoRepository(carpeta.resolve("catalogo.csv"))),
                    new LugaresNodosRepository(carpeta.resolve("lugares_nodos.csv")),
                    new MatrizRepository(carpeta.resolve("results/r2/matriz_tiempos.csv")),
                    new ResultadosRepository(carpeta.resolve("results")));
        }

        @Override Medicion medir(InstanciaDijkstra entrada, boolean lineal) {
            llamadas++;
            if (timeout && lineal) {
                return Medicion.noCompletada(List.of());
            }
            var resultado = lineal ? Dijkstra.lineal(entrada.grafo(), entrada.origen(), entrada.destino())
                    : Dijkstra.colaPrioridad(entrada.grafo(), entrada.origen(), entrada.destino());
            if (incorrecto) {
                resultado = new ResultadoDijkstra(999, resultado.ruta(), 1, resultado.distancias());
            }
            return new Medicion(List.of(lineal ? 4.0 : 1.0), true, resultado);
        }
    }

    private ExperimentoPrueba preparar() {
        List<List<Object>> nodos = new ArrayList<>();
        List<List<Object>> aristas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            nodos.add(List.of(i, 100 + i, 4.5, -75.7, i, i));
            aristas.add(List.of(i, (i + 1) % 8, 100, 60, "osm", 1, "residential"));
            aristas.add(List.of((i + 1) % 8, i, 100, 60, "osm", 3, "residential"));
        }
        Csv.escribir(carpeta.resolve("nodos.csv.gz"), RedRepository.COLUMNAS_NODOS, nodos);
        Csv.escribir(carpeta.resolve("aristas.csv.gz"), RedRepository.COLUMNAS_ARISTAS, aristas);
        List<List<Object>> catalogo = new ArrayList<>();
        List<FilaLugarNodo> asociaciones = new ArrayList<>();
        String[] categorias = {"cabecera", "atractivo", "finca_hotel", "acopio_trilladora"};
        int[] cantidades = {12, 15, 8, 5};
        int i = 0;
        for (int c = 0; c < categorias.length; c++) {
            for (int j = 0; j < cantidades[c]; j++, i++) {
                String id = i == 0 ? "cab_armenia" : "l" + i;
                catalogo.add(List.of(id, "Lugar", i < 20 ? "Armenia" : "Calarcá", categorias[c],
                        4.5, -75.7, c == 1 ? 60 : "", c == 1 ? "criterio" : "", "mapa", "si", ""));
                asociaciones.add(new FilaLugarNodo(id, i % 8, 0, false, ""));
            }
        }
        Csv.escribir(carpeta.resolve("catalogo.csv"), CatalogoRepository.COLUMNAS, catalogo);
        new LugaresNodosRepository(carpeta.resolve("lugares_nodos.csv")).guardar(asociaciones);
        return new ExperimentoPrueba();
    }

    @Test void t2ia_serieMatrizYCocientesReproducibles() {
        var experimento = preparar();
        experimento.ejecutar();
        assertEquals(30, experimento.llamadas);
        var pares = Csv.leer(carpeta.resolve("results/r2/pares.csv"));
        assertEquals(20, pares.size());
        assertEquals(10, pares.stream().filter(f -> f.get("criterio").equals("mismo_municipio")).count());
        assertTrue(pares.stream().allMatch(f -> f.get("semilla").equals("2026")));
        assertEquals(20, pares.stream().map(f -> f.get("origen") + ">" + f.get("destino")).distinct().count());
        assertEquals(30, Csv.leer(carpeta.resolve("results/r2/mediciones_dijkstra.csv")).size());
        assertEquals(10, Csv.leer(carpeta.resolve("results/r2/verificacion.csv")).size());
        var cocientes = Csv.leer(carpeta.resolve("results/r2/cociente.csv"));
        assertEquals(13, cocientes.size());
        assertTrue(cocientes.stream().allMatch(f -> Double.parseDouble(f.get("cociente")) == 4));
        var matriz = new MatrizRepository(carpeta.resolve("results/r2/matriz_tiempos.csv")).leer();
        assertEquals(40, matriz.ids().size());
        for (int i = 0; i < 40; i++) {
            assertEquals(0, matriz.matriz()[i][i]);
            for (int j = 0; j < 40; j++) {
                assertTrue(Double.isFinite(matriz.matriz()[i][j]));
            }
        }
        // Oráculo manual del ciclo: 0->1 cuesta 1, 1->0 cuesta 3.
        int uno = matriz.ids().indexOf("l1");
        int cero = matriz.ids().indexOf("cab_armenia");
        assertEquals(1, matriz.matriz()[cero][uno]);
        assertEquals(3, matriz.matriz()[uno][cero]);
        experimento.ejecutar();
        assertEquals(pares, Csv.leer(carpeta.resolve("results/r2/pares.csv")));
    }

    @Test void t2ib_asociacionesInvalidasNoMidenNiSobrescriben() throws Exception {
        var experimento = preparar();
        var archivo = carpeta.resolve("results/r2/pares.csv");
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, "anterior");
        new LugaresNodosRepository(carpeta.resolve("lugares_nodos.csv"))
                .guardar(List.of(new FilaLugarNodo("ajeno", 99, 0, false, "")));
        assertThrows(IllegalStateException.class, experimento::ejecutar);
        assertEquals(0, experimento.llamadas);
        assertEquals("anterior", Files.readString(archivo));
    }

    @Test void t2ib_grafoNoConexoNoMide() {
        var experimento = preparar();
        Csv.escribir(carpeta.resolve("aristas.csv.gz"), RedRepository.COLUMNAS_ARISTAS,
                List.of(List.of(0, 1, 100, 60, "osm", 1, "residential")));
        assertThrows(IllegalStateException.class, experimento::ejecutar);
        assertEquals(0, experimento.llamadas);
        assertFalse(Files.exists(carpeta.resolve("results/r2/matriz_tiempos.csv")));
    }

    @Test void t2ic_timeoutNoPublicaResultadoFicticio() {
        var experimento = preparar();
        experimento.timeout = true;
        experimento.ejecutar();
        var filas = Csv.leer(carpeta.resolve("results/r2/mediciones_dijkstra.csv"));
        var fallidas = filas.stream().filter(f -> f.get("estado").equals("no completada")).toList();
        assertEquals(10, fallidas.size());
        assertTrue(fallidas.stream().allMatch(f -> f.get("tiempo_ms").isEmpty()
                && f.get("tiempo_viaje_s").isEmpty() && f.get("ruta_nodos").isEmpty()));
        var cocientes = Csv.leer(carpeta.resolve("results/r2/cociente.csv"));
        assertTrue(cocientes.stream().allMatch(f -> f.get("cociente").isEmpty()));
    }

    @Test void t2ic_resultadoIncorrectoNoPublica() {
        var experimento = preparar();
        experimento.incorrecto = true;
        assertThrows(IllegalStateException.class, experimento::ejecutar);
        assertFalse(Files.exists(carpeta.resolve("results/r2/mediciones_dijkstra.csv")));
    }
}
