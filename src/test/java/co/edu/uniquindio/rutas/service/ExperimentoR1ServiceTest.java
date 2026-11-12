package co.edu.uniquindio.rutas.service;

import static org.junit.jupiter.api.Assertions.*;

import co.edu.uniquindio.rutas.repository.CatalogoRepository;
import co.edu.uniquindio.rutas.repository.Csv;
import co.edu.uniquindio.rutas.repository.LugaresNodosRepository;
import co.edu.uniquindio.rutas.repository.RedRepository;
import co.edu.uniquindio.rutas.repository.ResultadosRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExperimentoR1ServiceTest {
    @TempDir Path carpeta;

    private ExperimentoR1Service preparar(double primeraX) {
        escribirMetadatos("""
                {"nodos_descargada":3,"aristas_descargada":4,
                 "nodos_componente":2,"aristas_componente":1}
                """);
        Csv.escribir(carpeta.resolve("nodos.csv.gz"), RedRepository.COLUMNAS_NODOS,
                List.of(List.of(0, 100, 4.5, -75.7, 0, 0),
                        List.of(1, 200, 4.5, -75.7, 1000, 0)));
        Csv.escribir(carpeta.resolve("aristas.csv.gz"), RedRepository.COLUMNAS_ARISTAS,
                List.of(List.of(0, 1, 1000, 60, "osm", 60, "residential")));
        List<List<Object>> filas = new ArrayList<>();
        List<List<Object>> utm = new ArrayList<>();
        String[] categorias = {"cabecera", "atractivo", "finca_hotel", "acopio_trilladora"};
        int[] cantidades = {12, 15, 8, 5};
        int i = 0;
        for (int c = 0; c < categorias.length; c++) {
            for (int j = 0; j < cantidades[c]; j++) {
                String id = "l" + i;
                filas.add(List.of(id, "Lugar", "Armenia", categorias[c], 4.5, -75.7,
                        c == 1 ? 60 : "", c == 1 ? "criterio" : "", "mapa", "si",
                        "Conservar punto indicado por el grupo"));
                utm.add(List.of(id, i == 0 ? primeraX : i == 1 ? 500 : 0, 0));
                i++;
            }
        }
        Csv.escribir(carpeta.resolve("catalogo.csv"), CatalogoRepository.COLUMNAS, filas);
        Csv.escribir(carpeta.resolve("lugares_utm.csv"),
                List.of("id_lugar", "x_utm", "y_utm"), utm);
        return new ExperimentoR1Service(new RedService(new RedRepository(carpeta)),
                new CatalogoRepository(carpeta.resolve("catalogo.csv")),
                new LugaresNodosRepository(carpeta.resolve("lugares_nodos.csv")),
                new ResultadosRepository(carpeta.resolve("results")));
    }

    private void escribirMetadatos(String contenido) {
        try {
            Files.writeString(carpeta.resolve("metadatos.json"), contenido);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Test void t1ma_comparaTresRedesConParalelas() {
        var servicio = preparar(0);
        escribirMetadatos("""
                {"nodos_descargada":3,"aristas_descargada":4,
                 "nodos_componente":2,"aristas_componente":2}
                """);
        Csv.escribir(carpeta.resolve("aristas.csv.gz"), RedRepository.COLUMNAS_ARISTAS,
                List.of(List.of(0, 1, 1000, 60, "osm", 60, "residential"),
                        List.of(0, 1, 1000, 30, "osm", 120, "residential")));
        servicio.ejecutar();
        var filas = Csv.leer(carpeta.resolve("results/r1/memoria_representacion.csv"));
        assertEquals(List.of("red", "representacion", "supuesto_bytes", "V", "E", "megabytes"),
                List.copyOf(filas.getFirst().keySet()));
        assertEquals(6, filas.size());
        double[] bytes = {144, 104, 64, 56, 64, 36};
        String[] redes = {"descargada", "componente_exportada", "grafo_java"};
        for (int i = 0; i < filas.size(); i++) {
            assertEquals(redes[i / 2], filas.get(i).get("red"));
            assertEquals(i % 2 == 0 ? "matriz" : "listas", filas.get(i).get("representacion"));
            assertEquals(bytes[i] / 1_000_000, Double.parseDouble(filas.get(i).get("megabytes")));
            assertFalse(filas.get(i).get("supuesto_bytes").isBlank());
        }
        assertEquals("2", filas.get(2).get("E"));
        assertEquals("1", filas.get(4).get("E"));
    }

    @Test void t1mb_metadatosInvalidosNoSobrescriben() throws Exception {
        var servicio = preparar(0);
        var asociaciones = carpeta.resolve("lugares_nodos.csv");
        var memoria = carpeta.resolve("results/r1/memoria_representacion.csv");
        Files.createDirectories(memoria.getParent());
        Files.writeString(asociaciones, "asociaciones anteriores");
        Files.writeString(memoria, "memoria anterior");
        for (String invalido : List.of("{", "{}", "null", "[]", "\"texto\"", "-1", "0", "3.5", "2147483648")) {
            String json = invalido.startsWith("{") || invalido.equals("null") || invalido.equals("[]")
                    ? invalido : "{\"nodos_descargada\":" + invalido
                    + ",\"aristas_descargada\":4,\"nodos_componente\":2,\"aristas_componente\":1}";
            escribirMetadatos(json);
            assertThrows(IllegalStateException.class, servicio::ejecutar, invalido);
            assertEquals("asociaciones anteriores", Files.readString(asociaciones));
            assertEquals("memoria anterior", Files.readString(memoria));
        }
        Files.delete(carpeta.resolve("metadatos.json"));
        assertThrows(IllegalStateException.class, servicio::ejecutar);
        assertEquals("memoria anterior", Files.readString(memoria));
    }

    @Test void t1mc_conteosIncoherentesNoExportan() {
        var servicio = preparar(0);
        for (String json : List.of(
                "{\"nodos_descargada\":3,\"aristas_descargada\":4,\"nodos_componente\":3,\"aristas_componente\":1}",
                "{\"nodos_descargada\":3,\"aristas_descargada\":4,\"nodos_componente\":2,\"aristas_componente\":2}",
                "{\"nodos_descargada\":1,\"aristas_descargada\":4,\"nodos_componente\":2,\"aristas_componente\":1}",
                "{\"nodos_descargada\":3,\"aristas_descargada\":1,\"nodos_componente\":2,\"aristas_componente\":2}")) {
            escribirMetadatos(json);
            assertThrows(IllegalStateException.class, servicio::ejecutar);
            assertFalse(Files.exists(carpeta.resolve("lugares_nodos.csv")));
            assertFalse(Files.exists(carpeta.resolve("results/r1/memoria_representacion.csv")));
        }
    }

    @Test void t1md_productoCuadraticoNoDesborda() {
        var servicio = preparar(0);
        escribirMetadatos("""
                {"nodos_descargada":100000,"aristas_descargada":4,
                 "nodos_componente":2,"aristas_componente":1}
                """);
        servicio.ejecutar();
        var filas = Csv.leer(carpeta.resolve("results/r1/memoria_representacion.csv"));
        assertEquals(160000, Double.parseDouble(filas.getFirst().get("megabytes")));
    }

    @Test void t1ia_asociaUmbralEstrictoYTratamiento() {
        preparar(1700).ejecutar();
        var filas = new LugaresNodosRepository(carpeta.resolve("lugares_nodos.csv")).leer();
        assertEquals(40, filas.size());
        assertEquals(1, filas.getFirst().idxNodo());
        assertEquals(700, filas.getFirst().distanciaM());
        assertTrue(filas.getFirst().masDe500m());
        assertEquals("Conservar punto indicado por el grupo", filas.getFirst().tratamiento());
        assertEquals(0, filas.get(1).idxNodo()); // empate: menor índice
        assertEquals(500, filas.get(1).distanciaM());
        assertFalse(filas.get(1).masDe500m());
        assertEquals(1, Csv.leer(carpeta.resolve("results/r1/lugares_lejanos.csv")).size());
    }

    @Test void t1ib_sinLejanosExportaCabecera() throws Exception {
        preparar(0).ejecutar();
        var archivo = carpeta.resolve("results/r1/lugares_lejanos.csv");
        assertTrue(Csv.leer(archivo).isEmpty());
        assertEquals("id_lugar,idx_nodo,distancia_m,mas_de_500m,tratamiento",
                Files.readString(archivo).strip());
    }

    @Test void t1ic_utmFaltanteNoSobrescribe() throws Exception {
        var servicio = preparar(0);
        var salida = carpeta.resolve("lugares_nodos.csv");
        Files.writeString(salida, "anterior");
        Csv.escribir(carpeta.resolve("lugares_utm.csv"),
                List.of("id_lugar", "x_utm", "y_utm"), List.of(List.of("ajeno", 0, 0)));
        assertThrows(IllegalStateException.class, servicio::ejecutar);
        assertEquals("anterior", Files.readString(salida));
        assertFalse(Files.exists(carpeta.resolve("results/r1/lugares_lejanos.csv")));
    }

    @Test void t1ic_utmNoFinitaNoExporta() {
        var servicio = preparar(Double.NaN);
        assertThrows(IllegalStateException.class, servicio::ejecutar);
        assertFalse(Files.exists(carpeta.resolve("lugares_nodos.csv")));
    }

    @Test void t1ic_catalogoInvalidoNoExporta() {
        var servicio = preparar(0);
        Csv.escribir(carpeta.resolve("catalogo.csv"), CatalogoRepository.COLUMNAS, List.of());
        assertThrows(IllegalStateException.class, servicio::ejecutar);
        assertFalse(Files.exists(carpeta.resolve("lugares_nodos.csv")));
    }

    @Test void t1id_nodoNoFinitoNoExporta() {
        var servicio = preparar(0);
        Csv.escribir(carpeta.resolve("nodos.csv.gz"), RedRepository.COLUMNAS_NODOS,
                List.of(List.of(0, 100, 4.5, -75.7, Double.NaN, 0),
                        List.of(1, 200, 4.5, -75.7, 1000, 0)));
        assertThrows(IllegalStateException.class, servicio::ejecutar);
        assertFalse(Files.exists(carpeta.resolve("lugares_nodos.csv")));
    }

    @Test void t1ic_utmDuplicadaNoExporta() {
        var servicio = preparar(0);
        Csv.escribir(carpeta.resolve("lugares_utm.csv"),
                List.of("id_lugar", "x_utm", "y_utm"),
                List.of(List.of("l0", 0, 0), List.of("l0", 0, 0)));
        assertThrows(IllegalStateException.class, servicio::ejecutar);
        assertFalse(Files.exists(carpeta.resolve("lugares_nodos.csv")));
    }

    @Test void t1ia_lejanoSinNotasQuedaPendiente() {
        var servicio = preparar(1700);
        var filas = new ArrayList<>(Csv.leer(carpeta.resolve("catalogo.csv")));
        filas.getFirst().put("notas", "");
        Csv.escribir(carpeta.resolve("catalogo.csv"), CatalogoRepository.COLUMNAS,
                filas.stream().map(f -> CatalogoRepository.COLUMNAS.stream().map(f::get).toList())
                        .toList());
        servicio.ejecutar();
        var asociaciones = new LugaresNodosRepository(carpeta.resolve("lugares_nodos.csv")).leer();
        assertEquals("Pendiente: revisar acceso y justificar distancia al nodo",
                asociaciones.getFirst().tratamiento());
    }
}
